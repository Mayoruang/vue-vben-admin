#!/usr/bin/env python3
"""
InfluxDB Verification Script

This script connects to InfluxDB and verifies that drone telemetry data is being stored correctly.
It retrieves the most recent data points for drone telemetry and displays them.
"""

import sys
import json
from datetime import datetime, timedelta, timezone
from influxdb_client import InfluxDBClient

# InfluxDB connection parameters
INFLUXDB_URL = "http://localhost:8086"
INFLUXDB_TOKEN = "my-super-secret-token"
INFLUXDB_ORG = "drone_org"
INFLUXDB_BUCKET = "drone_data"

def verify_influxdb_data():
    """Connect to InfluxDB and verify that drone telemetry data exists"""
    
    print(f"Connecting to InfluxDB at {INFLUXDB_URL}...")
    
    # Create a client
    client = InfluxDBClient(url=INFLUXDB_URL, token=INFLUXDB_TOKEN, org=INFLUXDB_ORG)
    
    # Create query API
    query_api = client.query_api()
    
    # Get the current time and time 24 hours ago
    now = datetime.now(timezone.utc)
    one_day_ago = now - timedelta(days=1)
    
    # Format time range for Flux query
    start_time = one_day_ago.strftime('%Y-%m-%dT%H:%M:%SZ')
    end_time = now.strftime('%Y-%m-%dT%H:%M:%SZ')
    
    # First, check if there's any data in the bucket
    check_query = f'''
    from(bucket: "{INFLUXDB_BUCKET}")
        |> range(start: {start_time}, stop: {end_time})
        |> limit(n: 5)
    '''
    
    try:
        check_result = query_api.query(check_query)
        
        if not check_result:
            print("No data found in the InfluxDB bucket. Let's check the bucket list:")
            buckets = client.buckets_api().find_buckets().buckets
            print(f"Available buckets: {[b.name for b in buckets]}")
            
            # List measurements in the bucket
            measurements_query = f'''
            import "influxdata/influxdb/schema"
            schema.measurements(bucket: "{INFLUXDB_BUCKET}")
            '''
            try:
                measurements = query_api.query(measurements_query)
                if measurements:
                    print("Measurements in bucket:")
                    for table in measurements:
                        for record in table.records:
                            print(f"  - {record.values.get('_value')}")
                else:
                    print(f"No measurements found in bucket '{INFLUXDB_BUCKET}'")
            except Exception as e:
                print(f"Error querying measurements: {e}")
        
        # Try a specific query for drone telemetry
        telemetry_query = f'''
        from(bucket: "{INFLUXDB_BUCKET}")
            |> range(start: {start_time}, stop: {end_time})
            |> filter(fn: (r) => r._measurement == "drone_telemetry")
            |> sort(columns: ["_time"], desc: true)
            |> limit(n: 20)
        '''
        
        result = query_api.query(telemetry_query)
        
        if not result:
            print("\nNo drone telemetry data found. Let's try to find any data in the bucket.")
            
            # Try to find any data without filtering by measurement
            any_data_query = f'''
            from(bucket: "{INFLUXDB_BUCKET}")
                |> range(start: {start_time}, stop: {end_time})
                |> sort(columns: ["_time"], desc: true)
                |> limit(n: 20)
            '''
            
            any_data = query_api.query(any_data_query)
            
            if any_data:
                print("\n===== Found some data in InfluxDB =====")
                for table in any_data:
                    for record in table.records:
                        print(f"Measurement: {record.get_measurement()}")
                        print(f"Time: {record.get_time()}")
                        print(f"Field: {record.get_field()}")
                        print(f"Value: {record.get_value()}")
                        print(f"Tags: {record.values}")
                        print("---")
            else:
                print("\nNo data found in the InfluxDB bucket in the last 24 hours.")
                return False
        else:
            # Display the results
            print("\n===== Drone Telemetry Data in InfluxDB =====")
            
            records = []
            for table in result:
                for record in table.records:
                    records.append({
                        "time": record.get_time().strftime('%Y-%m-%d %H:%M:%S'),
                        "drone_id": record.values.get("drone_id", "unknown"),
                        "field": record.get_field(),
                        "value": record.get_value()
                    })
            
            # Group by time and drone_id
            grouped_data = {}
            for record in records:
                key = f"{record['time']}_{record['drone_id']}"
                if key not in grouped_data:
                    grouped_data[key] = {
                        "time": record['time'],
                        "drone_id": record['drone_id']
                    }
                grouped_data[key][record['field']] = record['value']
            
            # Print the grouped data
            for key, data in list(grouped_data.items())[:5]:  # Show only first 5 points
                print(f"\nTime: {data['time']}, Drone ID: {data['drone_id']}")
                for field, value in data.items():
                    if field not in ['time', 'drone_id']:
                        print(f"  {field}: {value}")
                        
            print("\nFound total of", len(grouped_data), "data points in InfluxDB")
            print("\n✅ Verification successful! InfluxDB is correctly storing drone telemetry data.")
            return True
            
    except Exception as e:
        print(f"Error querying InfluxDB: {str(e)}")
        return False
    finally:
        client.close()

if __name__ == "__main__":
    verify_influxdb_data() 