CREATE DATABASE IF NOT EXISTS logs_db;
USE logs_db;

CREATE TABLE IF NOT EXISTS api_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME(3) NOT NULL,
    level VARCHAR(20) NOT NULL,
    method VARCHAR(10) NOT NULL,
    path VARCHAR(255) NOT NULL,
    status_code INT,
    response_time_ms INT,
    user_id VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    request_body TEXT,
    response_body TEXT,
    error_message TEXT,
    trace_id VARCHAR(100),
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),

    INDEX idx_timestamp (timestamp),
    INDEX idx_path (path),
    INDEX idx_status_code (status_code),
    INDEX idx_trace_id (trace_id)
);