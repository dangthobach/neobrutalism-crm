-- PostgreSQL Replication Setup Script
-- This script creates a replication user and slot for streaming replication

-- Create replication user
CREATE USER replicator WITH REPLICATION ENCRYPTED PASSWORD 'repl_password_2024';

-- Create replication slot
SELECT * FROM pg_create_physical_replication_slot('replication_slot');

-- Grant necessary permissions
GRANT CONNECT ON DATABASE neobrutalism_crm TO replicator;

-- Update pg_hba.conf (this requires manual configuration or docker entrypoint script)
-- host replication replicator all md5

-- Display replication status
SELECT * FROM pg_stat_replication;
