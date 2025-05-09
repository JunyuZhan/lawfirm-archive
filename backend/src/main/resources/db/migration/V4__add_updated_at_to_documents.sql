-- 添加 updated_at 字段到 documents 表
ALTER TABLE documents ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

-- 使用当前的 upload_time 更新 updated_at 字段的初始值
UPDATE documents SET updated_at = upload_time WHERE updated_at IS NULL;

-- 创建触发器，自动更新 updated_at 字段
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_documents_updated_at
BEFORE UPDATE ON documents
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column(); 