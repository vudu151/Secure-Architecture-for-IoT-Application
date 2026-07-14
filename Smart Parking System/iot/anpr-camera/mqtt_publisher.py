import json
import ssl
import logging
import paho.mqtt.client as mqtt
from config import MQTT_HOST, MQTT_PORT, MQTT_USE_TLS, CA_CERT, CLIENT_CERT, CLIENT_KEY, DEVICE_UID, EVENT_TOPIC

logger = logging.getLogger("MQTTPublisher")


class MQTTPublisher:
    def __init__(self):
        import uuid
        unique_client_id = f"{DEVICE_UID}_{uuid.uuid4().hex[:8]}"
        self.client = mqtt.Client(client_id=unique_client_id, clean_session=True)
        self._connected = False

        if MQTT_USE_TLS:
            logger.info("Configuring mTLS certificates...")
            self.client.tls_set(
                ca_certs=CA_CERT,
                certfile=CLIENT_CERT,
                keyfile=CLIENT_KEY,
                cert_reqs=ssl.CERT_REQUIRED,
                tls_version=ssl.PROTOCOL_TLSv1_2,
            )
            self.client.tls_insecure_set(True)
        else:
            logger.info("Running without MQTT TLS (MQTT_USE_TLS is False).")

        self.client.on_connect = self._on_connect
        self.client.on_disconnect = self._on_disconnect

    def connect(self):
        logger.info(f"Connecting to MQTT broker at {MQTT_HOST}:{MQTT_PORT}...")
        self.client.connect(MQTT_HOST, MQTT_PORT, keepalive=60)
        self.client.loop_start()

    def disconnect(self):
        self.client.loop_stop()
        self.client.disconnect()
        logger.info("Disconnected from MQTT broker.")

    def publish_plate(self, plate_text: str, confidence: float = 1.0, image_url: str = None):
        import datetime
        timestamp = datetime.datetime.utcnow().isoformat() + "Z"
        data = {
            "event": "PLATE_DETECTED",
            "plate": plate_text,
            "confidence": round(confidence, 2),
            "timestamp": timestamp
        }
        if image_url:
            data["image_url"] = image_url
            
        payload = json.dumps(data)
        result = self.client.publish(EVENT_TOPIC, payload, qos=1)
        if result.rc == mqtt.MQTT_ERR_SUCCESS:
            logger.info(f"Published PLATE_DETECTED → topic='{EVENT_TOPIC}' plate='{plate_text}'")
        else:
            logger.error(f"Failed to publish message (rc={result.rc})")

    def _on_connect(self, client, userdata, flags, rc):
        if rc == 0:
            self._connected = True
            logger.info("MQTT connected successfully.")
        else:
            logger.error(f"MQTT connection failed with rc={rc}")

    def _on_disconnect(self, client, userdata, rc):
        self._connected = False
        logger.warning(f"MQTT disconnected (rc={rc}).")
