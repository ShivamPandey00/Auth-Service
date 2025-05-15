-- Create database
CREATE DATABASE auth_db;

-- Connect to the database
\c auth_db;

-- Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    user_type VARCHAR(20) NOT NULL
);

-- Create user_roles table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

-- Insert default admin user (password: Admin@123)
INSERT INTO users (email, password, full_name, phone_number, user_type)
VALUES ('admin@foodordering.com', '$2a$10$rDkPvvAFV6GgJkKq8WU1UOQZQZQZQZQZQZQZQZQZQZQZQZQZQZQZQ', 'Admin User', '+1234567890', 'ADMIN');

INSERT INTO user_roles (user_id, role)
VALUES (1, 'ADMIN'); 