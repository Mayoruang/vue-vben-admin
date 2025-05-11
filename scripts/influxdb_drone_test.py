#!/usr/bin/env python3
"""
Direct InfluxDB Drone Telemetry Test

This script directly writes drone telemetry data to InfluxDB 
using the line protocol format, similar to how the backend does it.
"""

import time
from datetime import datetime, timezone
from influxdb_client import InfluxDBClient, Point, WritePrecision

# InfluxDB connection parameters
INFLUXDB_URL = "http://localhost:8086"
INFLUXDB_TOKEN = "my-super-secret-token"
INFLUXDB_ORG = "drone_org"
INFLUXDB_BUCKET = "drone_data"
MEASUREMENT = "drone_telemetry"

def write_telemetry_data():
    """Directly write telemetry data to InfluxDB using line protocol"""
    
    print(f"Connecting to InfluxDB at {INFLUXDB_URL}...")
    
    # Create client
    client = InfluxDBClient(url=INFLUXDB_URL, token=INFLUXDB_TOKEN, org=INFLUXDB_ORG)
    
    try:
        # Write some test data points
        write_api = client.write_api()
        
        drone_id = "test-direct-final"
        current_time = datetime.now(timezone.utc)
        timestamp_nanos = int(current_time.timestamp() * 1000000000)
        
        # Create and write 10 data points using line protocol
        print("\nWriting telemetry data using line protocol...")
        
        for i in range(10):
            # Create a timestamp 10 seconds apart
            point_time = timestamp_nanos + i * 10000000000  # 10 seconds in nanoseconds
            
            # Create line protocol string directly
            line_protocol = (
                f"{MEASUREMENT},drone_id={drone_id} "
                f"battery_level={90.0-i*0.5},"
                f"battery_voltage={11.8-i*0.1},"
                f"latitude={22.543099+i*0.0001},"
                f"longitude={114.057868+i*0.0001},"
                f"altitude={50.0+i*5.0},"
                f"speed={5.0+i*0.5},"
                f"heading={120.0+i*2.0},"
                f"satellites={10},"
                f"signal_strength={95.0-i*1.0},"
                f"flight_mode=\"CRUISE\","
                f"temperature={28.5+i*0.1} {point_time}"
            )
            
            # Write to InfluxDB
            write_api.write(bucket=INFLUXDB_BUCKET, record=line_protocol, write_precision=WritePrecision.NS)
            print(f"✅ Written data point {i+1}/10 for drone {drone_id}")
            print(f"  Line Protocol: {line_protocol}")
            
            # Brief pause between writes
            time.sleep(0.5)
        
        # Close write API
        write_api.close()
        
        print("\n✅ All telemetry data points written successfully")
        
        # Verify data was written
        print("\nVerifying data was written to InfluxDB...")
        
        # Wait briefly for data to be indexed
        time.sleep(2)
        
        # Query the data back
        query_api = client.query_api()
        query = f'''
        from(bucket: "{INFLUXDB_BUCKET}")
            |> range(start: -1h)
            |> filter(fn: (r) => r._measurement == "{MEASUREMENT}")
            |> filter(fn: (r) => r.drone_id == "{drone_id}")
        '''
        
        result = query_api.query(query)
        
        if result:
            print("\n===== Verification: Drone Telemetry Data in InfluxDB =====")
            
            count = 0
            for table in result:
                for record in table.records:
                    time_str = record.get_time().strftime("%Y-%m-%d %H:%M:%S")
                    print(f"Time: {time_str}")
                    print(f"Field: {record.get_field()}")
                    print(f"Value: {record.get_value()}")
                    print(f"Drone ID: {record.values.get('drone_id')}")
                    print("---")
                    count += 1
            
            print(f"\nFound {count} drone telemetry data points for drone {drone_id}")
            print("\n✅ Test completed successfully! InfluxDB is storing drone telemetry data correctly.")
        else:
            print("\n❌ No drone telemetry data found in InfluxDB. There might be an issue with writing the data.")
            
    except Exception as e:
        print(f"\n❌ Error: {str(e)}")
    finally:
        client.close()

if __name__ == "__main__":
    write_telemetry_data() 