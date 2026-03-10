CREATE DATABASE jcloud;

USE jcloud;

-- USERS TABLE
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- FILES TABLE
CREATE TABLE files (
    file_id INT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    owner_id INT,
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(user_id)
);

-- CHUNKS TABLE
CREATE TABLE chunks (
    chunk_id INT AUTO_INCREMENT PRIMARY KEY,
    file_id INT,
    chunk_index INT,
    chunk_size INT,
    FOREIGN KEY (file_id) REFERENCES files(file_id)
);

-- DATA NODES TABLE
CREATE TABLE nodes (
    node_id INT AUTO_INCREMENT PRIMARY KEY,
    node_name VARCHAR(50),
    ip_address VARCHAR(50),
    port INT,
    status VARCHAR(20),
    storage_capacity BIGINT
);

-- CHUNK LOCATION TABLE
CREATE TABLE chunk_locations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    chunk_id INT,
    node_id INT,
    FOREIGN KEY (chunk_id) REFERENCES chunks(chunk_id),
    FOREIGN KEY (node_id) REFERENCES nodes(node_id)
);