-- Database initialization script for microservices architecture
-- Creates separate databases for each service

-- Create IAM database
CREATE DATABASE iam_db;

-- Create Keycloak database
CREATE DATABASE keycloak;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE iam_db TO crm_user;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO crm_user;

-- Connect to iam_db to create extensions
\c iam_db;

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create casbin tables will be handled by Flyway migrations
-- This is just for initial setup

COMMENT ON DATABASE iam_db IS 'IAM Service database for authentication and authorization';
COMMENT ON DATABASE keycloak IS 'Keycloak identity provider database';
