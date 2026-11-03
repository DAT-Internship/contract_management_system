CREATE TABLE password_reset_requests (
    request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_code VARCHAR(20) NOT NULL,
    reason VARCHAR(100) NOT NULL,
    details TEXT,
    status VARCHAR(20) DEFAULT 'PENDING', 
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_by BIGINT,
    resolved_at TIMESTAMP
);