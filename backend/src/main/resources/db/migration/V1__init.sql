CREATE TABLE IF NOT EXISTS files (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    storage_name VARCHAR(255) NOT NULL,
    path VARCHAR(1000),
    size BIGINT,
    type VARCHAR(100),
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    sort_order INTEGER DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_files_path ON files(path);
CREATE INDEX idx_files_upload_time ON files(upload_time);
CREATE INDEX idx_files_storage_name ON files(storage_name); 