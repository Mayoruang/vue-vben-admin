#!/usr/bin/env python3
"""
MQTT Test Script

This script directly publishes test telemetry data to the MQTT broker to verify
that the backend is receiving the messages and storing them in InfluxDB.
"""

import json
import time
import uuid
from datetime import datetime
import paho.mqtt.client as mqtt

# MQTT Configuration
MQTT_BROKER = "localhost"
MQTT_PORT = 1883
MQTT_CLIENT_ID = f"mqtt-test-{uuid.uuid4().hex[:8]}"

# Test drone ID - use a fixed ID for testing
TEST_DRONE_ID = "test-drone-123"
TELEMETRY_TOPIC = f"drones/{TEST_DRONE_ID}/telemetry"

def on_connect(client, userdata, flags, rc):
    """Callback when connected to MQTT broker"""
    if rc == 0:
        print(f"Connected to MQTT broker at {MQTT_BROKER}:{MQTT_PORT}")
    else:
        print(f"Failed to connect to MQTT broker, return code: {rc}")

def publish_test_data():
    """Connect to MQTT broker and publish test telemetry data"""
    
    print(f"MQTT Test - Publishing telemetry data for drone {TEST_DRONE_ID}")
    
    # Create MQTT client
    client = mqtt.Client(client_id=MQTT_CLIENT_ID)
    client.on_connect = on_connect
    
    try:
        # Connect to the broker
        client.connect(MQTT_BROKER, MQTT_PORT, 60)
        client.loop_start()
        
        # Wait for connection to establish
        time.sleep(1)
        
        # Publish 10 test messages
        for i in range(10):
            # Create test telemetry data
            telemetry_data = {
                "droneId": TEST_DRONE_ID,
                "timestamp": datetime.now().isoformat(),
                "batteryLevel": 90.5 - i * 0.5,
                "batteryVoltage": 11.8 - i * 0.1,
                "latitude": 22.543099 + i * 0.0001,
                "longitude": 114.057868 + i * 0.0001,
                "altitude": 50.0 + i * 5.0,
                "speed": 5.0 + i * 0.5,
                "heading": 120.0 + i * 2.0,
                "satellites": 10,
                "signalStrength": 95.0 - i * 1.0,
                "flightMode": "CRUISE",
                "temperature": 28.5 + i * 0.1
            }
            
            # Convert to JSON and publish
            payload = json.dumps(telemetry_data)
            result = client.publish(TELEMETRY_TOPIC, payload, qos=1)
            
            # Check publish status
            if result.rc == 0:
                print(f"✅ Message {i+1}/10 published successfully")
                print(f"  Topic: {TELEMETRY_TOPIC}")
                print(f"  Payload: {payload}")
            else:
                print(f"❌ Failed to publish message {i+1}/10: {result.rc}")
            
            # Wait for a second before sending next message
            time.sleep(1)
        
        # Wait a little after the last message
        time.sleep(2)
        print("\nAll test messages published. Now run the verify_influxdb.py script to check if data is stored.")
        
    except Exception as e:
        print(f"Error: {str(e)}")
    finally:
        client.loop_stop()
        client.disconnect()

if __name__ == "__main__":
    publish_test_data() 