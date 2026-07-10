import paho.mqtt.client as mqtt
import json

# Local Mosquitto (Docker)
LOCAL_BROKER = "localhost"
LOCAL_PORT = 1883

# Remote HiveMQ (Wokwi)
REMOTE_BROKER = "broker.hivemq.com"
REMOTE_PORT = 1883
PREFIX = "smartpkg/"

local_client = mqtt.Client(client_id="python_bridge_local")
remote_client = mqtt.Client(client_id="python_bridge_remote_12345")

def on_remote_connect(client, userdata, flags, rc):
    print("Connected to HiveMQ with result code " + str(rc))
    client.subscribe(PREFIX + "parking/slots/+/status")
    client.subscribe(PREFIX + "parking/devices/+/heartbeat")

def on_local_connect(client, userdata, flags, rc):
    print("Connected to Local Mosquitto with result code " + str(rc))
    client.subscribe("parking/slots/+/command")
    client.subscribe("parking/gates/+/control")

def on_remote_message(client, userdata, msg):
    # Remove prefix and forward to local
    local_topic = msg.topic[len(PREFIX):]
    print(f"HiveMQ -> Local: {local_topic} {msg.payload}")
    local_client.publish(local_topic, msg.payload)

def on_local_message(client, userdata, msg):
    # Add prefix and forward to remote
    remote_topic = PREFIX + msg.topic
    print(f"Local -> HiveMQ: {remote_topic} {msg.payload}")
    remote_client.publish(remote_topic, msg.payload)

remote_client.on_connect = on_remote_connect
remote_client.on_message = on_remote_message

local_client.on_connect = on_local_connect
local_client.on_message = on_local_message

print("Starting Python MQTT Bridge...")
print("Connecting to local broker...")
local_client.connect(LOCAL_BROKER, LOCAL_PORT, 60)
print("Connecting to remote broker...")
remote_client.connect(REMOTE_BROKER, REMOTE_PORT, 60)

local_client.loop_start()
remote_client.loop_forever()
