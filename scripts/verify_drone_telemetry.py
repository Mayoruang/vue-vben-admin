#!/usr/bin/env python3
"""
InfluxDB Drone Telemetry Verification Script

This script specifically checks for data in the drone_telemetry measurement
to verify that it's being stored correctly in InfluxDB.
"""

from datetime import datetime, timedelta, timezone
from influxdb_client import InfluxDBClient

# InfluxDB connection parameters
INFLUXDB_URL = "http://localhost:8086"
INFLUXDB_TOKEN = "my-super-secret-token"
INFLUXDB_ORG = "drone_org"
INFLUXDB_BUCKET = "drone_data"

def verify_drone_telemetry_data():
    """Connect to InfluxDB and specifically search for drone_telemetry data"""
    
    print(f"Connecting to InfluxDB at {INFLUXDB_URL}...")
    
    # Create client
    client = InfluxDBClient(url=INFLUXDB_URL, token=INFLUXDB_TOKEN, org=INFLUXDB_ORG)
    
    try:
        # Get the current time and time 24 hours ago
        now = datetime.now(timezone.utc)
        one_day_ago = now - timedelta(days=1)
        
        # Format time range for Flux query
        start_time = one_day_ago.strftime('%Y-%m-%dT%H:%M:%SZ')
        end_time = now.strftime('%Y-%m-%dT%H:%M:%SZ')
        
        # Create query API
        query_api = client.query_api()
        
        # First, check all measurements in the bucket
        measurements_query = '''
        import "influxdata/influxdb/schema"
        schema.measurements(bucket: "drone_data")
        '''
        
        measurements = query_api.query(measurements_query)
        if measurements:
            print("\nAvailable measurements in bucket:")
            for table in measurements:
                for record in table.records:
                    print(f"  - {record.values.get('_value')}")
        else:
            print("\nNo measurements found in bucket")
        
        # Now query specifically for drone_telemetry data
        query = f'''
        from(bucket: "{INFLUXDB_BUCKET}")
            |> range(start: {start_time}, stop: {end_time})
            |> filter(fn: (r) => r._measurement == "drone_telemetry")
        '''
        
        print("\nQuerying for drone_telemetry data...")
        result = query_api.query(query)
        
        if not result:
            print("No drone_telemetry data found in InfluxDB.")
            
            # Try a query with no filters to see if anything is there
            any_data_query = f'''
            from(bucket: "{INFLUXDB_BUCKET}")
                |> range(start: {start_time}, stop: {end_time})
                |> limit(n: 10)
            '''
            
            print("\nQuerying for any data in the bucket...")
            any_data = query_api.query(any_data_query)
            
            if any_data:
                print("\nFound some data in InfluxDB, but not drone_telemetry. Sample data:")
                for table in any_data:
                    for record in table.records:
                        print(f"Measurement: {record.get_measurement()}")
                        print(f"Time: {record.get_time()}")
                        print(f"Field: {record.get_field()}")
                        print(f"Value: {record.get_value()}")
                        print("---")
            else:
                print("No data found in the bucket in the specified time range.")
            
            return False
        
        # Display the drone telemetry data
        print("\n===== Drone Telemetry Data in InfluxDB =====")
        
        records = []
        for table in result:
            for record in table.records:
                print(f"Time: {record.get_time()}")
                print(f"Field: {record.get_field()}")
                print(f"Value: {record.get_value()}")
                print(f"Drone ID: {record.values.get('drone_id', 'unknown')}")
                print("---")
                
                records.append({
                    "time": record.get_time(),
                    "drone_id": record.values.get("drone_id", "unknown"),
                    "field": record.get_field(),
                    "value": record.get_value()
                })
        
        print(f"\nFound {len(records)} drone telemetry data points in InfluxDB")
        
        if records:
            print("\n✅ Verification successful! InfluxDB is correctly storing drone telemetry data.")
            return True
        else:
            print("\n❌ No drone telemetry data found despite query returning results.")
            return False
            
    except Exception as e:
        print(f"Error querying InfluxDB: {str(e)}")
        return False
    finally:
        client.close()

if __name__ == "__main__":
    verify_drone_telemetry_data() 