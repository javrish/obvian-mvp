-- Database initialization script for Obvian Petri Net POC
-- This script sets up the basic database structure for the proof-of-concept

-- Create database if not exists (handled by POSTGRES_DB env var)
-- This file is run automatically when the PostgreSQL container starts

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create schemas
CREATE SCHEMA IF NOT EXISTS obvian_core;
CREATE SCHEMA IF NOT EXISTS obvian_audit;

-- Set default search path
ALTER DATABASE obvian_poc SET search_path TO obvian_core, public;

-- Create basic tables for POC
-- These will be managed by JPA/Hibernate in the application

-- User management (basic for POC)
CREATE TABLE IF NOT EXISTS obvian_core.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT true
);

-- Execution history
CREATE TABLE IF NOT EXISTS obvian_core.executions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES obvian_core.users(id),
    dag_definition JSONB NOT NULL,
    execution_status VARCHAR(50) DEFAULT 'PENDING',
    start_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP WITH TIME ZONE,
    results JSONB,
    trace_data JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Petri Net storage
CREATE TABLE IF NOT EXISTS obvian_core.petri_nets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    user_id UUID REFERENCES obvian_core.users(id),
    network_definition JSONB NOT NULL,
    initial_marking JSONB,
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Plugin configurations
CREATE TABLE IF NOT EXISTS obvian_core.plugin_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plugin_id VARCHAR(255) NOT NULL,
    user_id UUID REFERENCES obvian_core.users(id),
    configuration JSONB NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Memory store (for POC memory management)
CREATE TABLE IF NOT EXISTS obvian_core.memory_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES obvian_core.users(id),
    key VARCHAR(255) NOT NULL,
    value JSONB NOT NULL,
    entry_type VARCHAR(100),
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Audit log for POC compliance
CREATE TABLE IF NOT EXISTS obvian_audit.activity_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID,
    action VARCHAR(255) NOT NULL,
    resource_type VARCHAR(100),
    resource_id UUID,
    details JSONB,
    ip_address INET,
    user_agent TEXT,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_email ON obvian_core.users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON obvian_core.users(username);
CREATE INDEX IF NOT EXISTS idx_executions_user_id ON obvian_core.executions(user_id);
CREATE INDEX IF NOT EXISTS idx_executions_status ON obvian_core.executions(execution_status);
CREATE INDEX IF NOT EXISTS idx_executions_start_time ON obvian_core.executions(start_time);
CREATE INDEX IF NOT EXISTS idx_petri_nets_user_id ON obvian_core.petri_nets(user_id);
CREATE INDEX IF NOT EXISTS idx_petri_nets_name ON obvian_core.petri_nets(name);
CREATE INDEX IF NOT EXISTS idx_plugin_configs_user_id ON obvian_core.plugin_configs(user_id);
CREATE INDEX IF NOT EXISTS idx_plugin_configs_plugin_id ON obvian_core.plugin_configs(plugin_id);
CREATE INDEX IF NOT EXISTS idx_memory_entries_user_key ON obvian_core.memory_entries(user_id, key);
CREATE INDEX IF NOT EXISTS idx_memory_expires ON obvian_core.memory_entries(expires_at);
CREATE INDEX IF NOT EXISTS idx_activity_log_user_id ON obvian_audit.activity_log(user_id);
CREATE INDEX IF NOT EXISTS idx_activity_log_timestamp ON obvian_audit.activity_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_activity_log_action ON obvian_audit.activity_log(action);

-- Create triggers for updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply triggers to tables with updated_at columns
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON obvian_core.users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_petri_nets_updated_at BEFORE UPDATE ON obvian_core.petri_nets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_plugin_configs_updated_at BEFORE UPDATE ON obvian_core.plugin_configs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_memory_entries_updated_at BEFORE UPDATE ON obvian_core.memory_entries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert POC demo user
INSERT INTO obvian_core.users (username, email)
VALUES ('poc_user', 'poc@obvian.com')
ON CONFLICT (username) DO NOTHING;

-- Create a view for monitoring POC limits
CREATE OR REPLACE VIEW obvian_core.poc_network_stats AS
SELECT
    pn.id,
    pn.name,
    pn.user_id,
    COALESCE(jsonb_array_length(pn.network_definition->'places'), 0) as place_count,
    COALESCE(jsonb_array_length(pn.network_definition->'transitions'), 0) as transition_count,
    COALESCE(jsonb_array_length(pn.network_definition->'arcs'), 0) as arc_count,
    CASE
        WHEN COALESCE(jsonb_array_length(pn.network_definition->'places'), 0) > 30 THEN 'EXCEEDS_PLACE_LIMIT'
        WHEN COALESCE(jsonb_array_length(pn.network_definition->'transitions'), 0) > 30 THEN 'EXCEEDS_TRANSITION_LIMIT'
        ELSE 'WITHIN_LIMITS'
    END as poc_status,
    pn.created_at,
    pn.updated_at
FROM obvian_core.petri_nets pn;

-- Create function to clean up expired memory entries
CREATE OR REPLACE FUNCTION clean_expired_memory() RETURNS void AS $$
BEGIN
    DELETE FROM obvian_core.memory_entries
    WHERE expires_at IS NOT NULL AND expires_at < CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- Grant permissions
GRANT USAGE ON SCHEMA obvian_core TO obvian;
GRANT USAGE ON SCHEMA obvian_audit TO obvian;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA obvian_core TO obvian;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA obvian_audit TO obvian;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA obvian_core TO obvian;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA obvian_audit TO obvian;

-- Set up row-level security for multi-tenancy (POC level)
ALTER TABLE obvian_core.executions ENABLE ROW LEVEL SECURITY;
ALTER TABLE obvian_core.petri_nets ENABLE ROW LEVEL SECURITY;
ALTER TABLE obvian_core.plugin_configs ENABLE ROW LEVEL SECURITY;
ALTER TABLE obvian_core.memory_entries ENABLE ROW LEVEL SECURITY;

-- Create policies (basic for POC)
CREATE POLICY user_executions ON obvian_core.executions
    FOR ALL TO obvian
    USING (user_id = current_setting('app.user_id')::uuid);

CREATE POLICY user_petri_nets ON obvian_core.petri_nets
    FOR ALL TO obvian
    USING (user_id = current_setting('app.user_id')::uuid);

CREATE POLICY user_plugin_configs ON obvian_core.plugin_configs
    FOR ALL TO obvian
    USING (user_id = current_setting('app.user_id')::uuid);

CREATE POLICY user_memory_entries ON obvian_core.memory_entries
    FOR ALL TO obvian
    USING (user_id = current_setting('app.user_id')::uuid);

-- POC warning comments
COMMENT ON SCHEMA obvian_core IS 'Obvian Petri Net POC - Core application schema';
COMMENT ON SCHEMA obvian_audit IS 'Obvian Petri Net POC - Audit and compliance schema';
COMMENT ON TABLE obvian_core.petri_nets IS 'POC: Limited to networks with ≤30 places and ≤30 transitions';
COMMENT ON VIEW obvian_core.poc_network_stats IS 'POC: Monitor network size compliance';
COMMENT ON FUNCTION clean_expired_memory() IS 'POC: Cleanup function for memory management';

-- Initialize database statistics
ANALYZE;