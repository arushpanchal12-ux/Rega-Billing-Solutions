-- Performance indexes for high-load scenarios
CREATE INDEX IF NOT EXISTS idx_pre_customers_email_status ON PRE_CUSTOMERS(email, status);
CREATE INDEX IF NOT EXISTS idx_pre_customers_created_at ON PRE_CUSTOMERS(created_at);
CREATE INDEX IF NOT EXISTS idx_users_email_status ON USERS(email, status);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON USERS(created_at);

-- Additional performance indexes
CREATE INDEX IF NOT EXISTS idx_pre_customers_phone ON PRE_CUSTOMERS(phone);
CREATE INDEX IF NOT EXISTS idx_users_email ON USERS(email);
CREATE INDEX IF NOT EXISTS idx_users_pre_customer_id ON USERS(pre_customer_id);
