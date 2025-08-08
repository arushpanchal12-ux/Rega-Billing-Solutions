-- Pre-customers table for decoy signup tracking
CREATE TABLE pre_customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50),
    status ENUM('PENDING_PAYMENT','EXPIRED','CONVERTED') NOT NULL DEFAULT 'PENDING_PAYMENT',
    marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    password_hash VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_payment_attempt_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- Activated users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pre_customer_id BIGINT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50) UNIQUE,
    password_hash VARCHAR(255) NULL,
    status ENUM('ACTIVE_PAID','SUSPENDED','DELETED') NOT NULL DEFAULT 'ACTIVE_PAID',
    oauth_google_sub VARCHAR(255) UNIQUE NULL,
    oauth_ms_sub VARCHAR(255) UNIQUE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    CONSTRAINT fk_users_pre_customer FOREIGN KEY (pre_customer_id) REFERENCES pre_customers(id),
    INDEX idx_email (email),
    INDEX idx_status (status)
);
