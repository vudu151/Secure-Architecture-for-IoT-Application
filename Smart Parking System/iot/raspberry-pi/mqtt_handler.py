import json
import time
import logging
import requests
import ssl
import threading
import paho.mqtt.client as mqtt
from config import (
    MQTT_HOST, MQTT_PORT, CA_CERT, CLIENT_CERT, CLIENT_KEY,
    GATE_ID, DEVICE_UID, FIRMWARE_VER, API_BASE_URL
)

logger = logging.getLogger("MQTTHandler")

class MQTTHandler:
    def __init__(self, gate_controller):
        self.gate_controller = gate_controller
        self.client = mqtt.Client(client_id=DEVICE_UID, clean_session=True)
        self.control_topic = f"parking/gates/{GATE_ID}/control"
        self.event_topic = f"parking/gates/{GATE_ID}/event"
        self.heartbeat_topic = f"parking/devices/{DEVICE_UID}/heartbeat"
        
        # Setup TLS certificates for mTLS
        logger.info("Configuring mTLS certificates...")
        try:
            self.client.tls_set(
                ca_certs=CA_CERT,
                certfile=CLIENT_CERT,
                keyfile=CLIENT_KEY,
                cert_reqs=ssl.CERT_REQUIRED,
                tls_version=ssl.PROTOCOL_TLSv1_2
            )
            # Insecure set allows localhost connection even if cert CN is "mosquitto"
            self.client.tls_insecure_set(True)
            logger.info("mTLS configured successfully.")
        except Exception as e:
            logger.error(f"Failed to configure mTLS: {e}")
            raise e

        # Set callbacks
        self.client.on_connect = self.on_connect
        self.client.on_disconnect = self.on_disconnect
        self.client.on_message = self.on_message

        self.heartbeat_thread = None
        self.stop_heartbeat = threading.Event()

    def connect(self):
        """
        Connects to the MQTT broker and starts the background loop.
        """
        logger.info(f"Connecting to MQTT Broker at {MQTT_HOST}:{MQTT_PORT}...")
        try:
            self.client.connect(MQTT_HOST, MQTT_PORT, keepalive=60)
            self.client.loop_start()
            
            # Start heartbeat thread
            self.stop_heartbeat.clear()
            self.heartbeat_thread = threading.Thread(target=self.heartbeat_loop, daemon=True)
            self.heartbeat_thread.start()
        except Exception as e:
            logger.error(f"Failed to connect to MQTT broker: {e}")
            raise e

    def disconnect(self):
        """
        Gracefully disconnects from the MQTT broker.
        """
        logger.info("Disconnecting from MQTT broker...")
        self.stop_heartbeat.set()
        self.client.loop_stop()
        self.client.disconnect()
        logger.info("Disconnected from MQTT broker.")

    def on_connect(self, client, userdata, flags, rc):
        if rc == 0:
            logger.info(f"Connected to MQTT broker successfully (rc={rc}).")
            # Subscribe to the control topic
            logger.info(f"Subscribing to control topic: {self.control_topic}")
            self.client.subscribe(self.control_topic, qos=2)
        else:
            logger.error(f"Failed to connect to MQTT broker, return code {rc}")

    def on_disconnect(self, client, userdata, rc):
        logger.warning(f"Disconnected from MQTT broker with return code: {rc}. Reconnecting...")
        # Paho automatically handles reconnection if loop_start() is running, but let's log it

    def on_message(self, client, userdata, msg):
        """
        Callback triggered when a control command is received from the backend.
        """
        logger.info(f"Received message on topic: {msg.topic}")
        try:
            payload = json.loads(msg.payload.decode('utf-8'))
            logger.info(f"Control Payload: {payload}")
            
            action = payload.get("action")
            nonce = payload.get("nonce")
            timestamp = payload.get("timestamp")

            if not action or not nonce or not timestamp:
                logger.warning("Missing required fields (action, nonce, timestamp) in control payload.")
                return

            # 1. Local Verification: Check if timestamp is within local allowed window (5 seconds)
            current_time_ms = int(time.time() * 1000)
            time_difference = abs(current_time_ms - timestamp)
            logger.info(f"Checking timestamp: current_time={current_time_ms}, msg_time={timestamp}, diff={time_difference}ms")
            
            if time_difference > 5000:
                logger.warning(f"[REPLAY ATTACK WARNING] Message timestamp too old! Diff: {time_difference}ms (limit: 5000ms). Ignoring command.")
                return

            # 2. Remote Verification: Send HTTP request to Backend to verify the nonce (anti-replay + validity)
            logger.info(f"Verifying nonce {nonce} with backend API...")
            verify_url = f"{API_BASE_URL}/devices/verify-nonce"
            response = requests.post(verify_url, params={"nonce": nonce, "timestamp": timestamp}, timeout=3.0)
            
            if response.status_code == 200:
                logger.info(f"Nonce {nonce} validated successfully by backend. Executing command...")
                # 3. Execute gate action
                if action == "OPEN":
                    self.gate_controller.open_gate()
                elif action == "CLOSE":
                    self.gate_controller.close_gate()
                else:
                    logger.warning(f"Unknown action: {action}")
            else:
                logger.warning(f"[REPLAY OR INVALID NONCE] Backend rejected nonce validation! HTTP Status: {response.status_code}, Body: {response.text}")
        except Exception as e:
            logger.error(f"Error handling MQTT control message: {e}")

    def publish_event(self, event_type, data):
        """
        Publishes a gate event to the backend.
        event_type: "PLATE_DETECTED" or "QR_SCANNED"
        data: license plate string or decoded QR data string
        """
        payload = {}
        if event_type == "PLATE_DETECTED":
            payload = {
                "event": "PLATE_DETECTED",
                "plate": data
            }
        elif event_type == "QR_SCANNED":
            payload = {
                "event": "QR_SCANNED",
                "qrData": data
            }
        else:
            logger.warning(f"Unknown event type: {event_type}")
            return

        try:
            json_payload = json.dumps(payload)
            logger.info(f"Publishing event to {self.event_topic}: {json_payload}")
            self.client.publish(self.event_topic, json_payload, qos=1)
        except Exception as e:
            logger.error(f"Failed to publish event: {e}")

    def heartbeat_loop(self):
        """
        Sends heartbeat updates to the broker every 60 seconds.
        """
        while not self.stop_heartbeat.is_set():
            try:
                payload = {
                    "firmwareVersion": FIRMWARE_VER
                }
                json_payload = json.dumps(payload)
                logger.debug(f"Publishing heartbeat: {json_payload}")
                self.client.publish(self.heartbeat_topic, json_payload, qos=0)
            except Exception as e:
                logger.error(f"Failed to send heartbeat: {e}")
            
            # Wait for 60 seconds, check for stop signal every 1 second
            for _ in range(60):
                if self.stop_heartbeat.is_set():
                    break
                time.sleep(1)
