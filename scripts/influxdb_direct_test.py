#!/usr/bin/env python3
"""
InfluxDB Direct Test Script

This script bypasses the backend and directly writes test data to InfluxDB 
to verify that the database is properly configured and can store data.
"""

import time
from datetime import datetime, timedelta, timezone
from influxdb_client import InfluxDBClient, Point, WritePrecision

# InfluxDB connection parameters - match application.yml settings
INFLUXDB_URL = "http://localhost:8086"
INFLUXDB_TOKEN = "my-super-secret-token"
INFLUXDB_ORG = "drone_org"
INFLUXDB_BUCKET = "drone_data"

def test_influxdb_write():
    """Connect to InfluxDB and write some test data directly"""
    
    print(f"Connecting to InfluxDB at {INFLUXDB_URL}...")
    
    # Create client
    client = InfluxDBClient(url=INFLUXDB_URL, token=INFLUXDB_TOKEN, org=INFLUXDB_ORG)
    
    # Check the connection
    try:
        ready = client.ping()
        if ready:
            print("✅ InfluxDB connection successful!")
        else:
            print("❌ InfluxDB ping failed!")
            return False
    except Exception as e:
        print(f"❌ InfluxDB connection error: {str(e)}")
        return False
    
    # List buckets to verify access
    try:
        buckets_api = client.buckets_api()
        buckets = buckets_api.find_buckets().buckets
        bucket_names = [b.name for b in buckets]
        print(f"Available buckets: {bucket_names}")
        
        if INFLUXDB_BUCKET not in bucket_names:
            print(f"❌ Target bucket '{INFLUXDB_BUCKET}' not found in available buckets!")
            print("Creating bucket...")
            new_bucket = buckets_api.create_bucket(bucket_name=INFLUXDB_BUCKET, org=INFLUXDB_ORG)
            print(f"✅ Created bucket: {new_bucket.name}")
    except Exception as e:
        print(f"❌ Error listing/creating buckets: {str(e)}")
    
    # Write test data
    try:
        print("\nWriting test data points to InfluxDB...")
        write_api = client.write_api()
        
        # Create test data points spanning different times
        current_time = datetime.now(timezone.utc)
        
        for i in range(5):
            # Create a data point for i minutes ago
            point_time = current_time - timedelta(minutes=i)
            
            # Create point with a different test measurement name
            point = Point("test_measurement") \
                .tag("test_id", f"test-{i}") \
                .field("value", 100 - i * 10) \
                .time(point_time, WritePrecision.NS)
            
            write_api.write(bucket=INFLUXDB_BUCKET, record=point)
            print(f"✅ Written test point {i+1} with time {point_time}")
        
        # Sleep briefly to allow data to be written
        time.sleep(1)
        
        # Now focus on writing and verifying drone_telemetry data
        print("\n=== Testing drone_telemetry measurement specifically ===")
        
        # Create a test drone telemetry point
        drone_id = "test-direct-456"
        print(f"Creating drone_telemetry point for drone_id: {drone_id}")
        
        drone_point = Point("drone_telemetry") \
            .tag("drone_id", drone_id) \
            .field("battery_level", 85.5) \
            .field("latitude", 22.543099) \
            .field("longitude", 114.057868) \
            .field("altitude", 100.0) \
            .field("temperature", 28.5) \
            .time(current_time, WritePrecision.NS)
        
        # Write the drone telemetry point
        print(f"Writing drone_telemetry point to bucket: {INFLUXDB_BUCKET}")
        write_result = write_api.write(bucket=INFLUXDB_BUCKET, org=INFLUXDB_ORG, record=drone_point)
        print(f"Write result: {write_result}")
        
        # Create a second test point with different fields
        print("Creating second drone_telemetry point to verify consistency")
        drone_point2 = Point("drone_telemetry") \
            .tag("drone_id", drone_id) \
            .field("speed", 15.2) \
            .field("heading", 123.5) \
            .field("battery_voltage", 11.8) \
            .time(current_time + timedelta(seconds=5), WritePrecision.NS)
        
        write_result2 = write_api.write(bucket=INFLUXDB_BUCKET, org=INFLUXDB_ORG, record=drone_point2)
        print(f"Second write result: {write_result2}")
        
        # Close write API
        write_api.close()
        
        print("\n✅ All test points written successfully")
        print("Waiting for data to be stored (5 seconds)...")
        time.sleep(5)
        
        # Try to verify specifically the drone_telemetry data
        query_api = client.query_api()
        
        # First check what measurements exist in the bucket
        measurements_query = '''
        import "influxdata/influxdb/schema"
        schema.measurements(bucket: "drone_data")
        '''
        
        print("\nChecking measurements in the bucket...")
        measurements = query_api.query(measurements_query)
        if measurements:
            print("Measurements found in bucket:")
            for table in measurements:
                for record in table.records:
                    print(f"  - {record.values.get('_value')}")
        else:
            print("No measurements found in bucket")
        
        # Query specifically for drone_telemetry data
        drone_query = f'''
        from(bucket: "{INFLUXDB_BUCKET}")
            |> range(start: -10m)
            |> filter(fn: (r) => r._measurement == "drone_telemetry")
        '''
        
        print("\nQuerying specifically for drone_telemetry data...")
        drone_result = query_api.query(drone_query)
        
        if drone_result:
            print("\n===== Retrieved drone_telemetry data from InfluxDB =====")
            for table in drone_result:
                for record in table.records:
                    print(f"Time: {record.get_time()}, Field: {record.get_field()}, Value: {record.get_value()}")
                    print(f"Drone ID: {record.values.get('drone_id', 'unknown')}")
                    print("---")
            print("\n✅ Success! drone_telemetry data was stored and retrieved.")
        else:
            print("\n❌ No drone_telemetry data found. Query returned no results.")
            print("Trying a more generic query for recent data...")
            
            any_query = f'''
            from(bucket: "{INFLUXDB_BUCKET}")
                |> range(start: -10m)
                |> limit(n: 10)
            '''
            
            any_result = query_api.query(any_query)
            if any_result:
                print("\nFound some data, but no drone_telemetry:")
                for table in any_result:
                    for record in table.records:
                        print(f"Measurement: {record.get_measurement()}")
                        print(f"Time: {record.get_time()}")
                        print(f"Field: {record.get_field()}")
                        print(f"Value: {record.get_value()}")
                        print("---")
            else:
                print("No data found at all in the last 10 minutes")
            
            # Attempt to diagnose by trying to force a specific write format
            print("\nAttempting one more write of drone_telemetry with line protocol...")
            # Create a line protocol string directly
            line_protocol = f'drone_telemetry,drone_id={drone_id} battery_level=88.5,altitude=120.0 {int(datetime.now(timezone.utc).timestamp() * 1000000000)}'
            
            try:
                write_api = client.write_api()
                write_api.write(bucket=INFLUXDB_BUCKET, org=INFLUXDB_ORG, record=line_protocol)
                print("Line protocol write completed")
                write_api.close()
                
                print("Waiting for line protocol data (3 seconds)...")
                time.sleep(3)
                
                print("Querying again for drone_telemetry...")
                final_result = query_api.query(drone_query)
                
                if final_result:
                    print("✅ Success! Line protocol drone_telemetry data was stored and retrieved.")
                    for table in final_result:
                        for record in table.records:
                            print(f"Time: {record.get_time()}, Field: {record.get_field()}, Value: {record.get_value()}")
                            print(f"Drone ID: {record.values.get('drone_id', 'unknown')}")
                            print("---")
                else:
                    print("❌ Still no drone_telemetry data found. Issue persists.")
            except Exception as e:
                print(f"❌ Error with line protocol write: {str(e)}")
                
            return False
        
        return True
        
    except Exception as e:
        print(f"❌ Error writing/reading test data: {str(e)}")
        return False
    finally:
        client.close()

if __name__ == "__main__":
    test_influxdb_write() 