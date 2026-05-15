CREATE TABLE IF NOT EXISTS admin_console_users (
    admin_user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    display_name VARCHAR(160),
    role VARCHAR(80) NOT NULL DEFAULT 'admin',
    status VARCHAR(24) NOT NULL DEFAULT 'active',
    created_by VARCHAR(255),
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT admin_console_users_status_check CHECK (status IN ('active', 'suspended', 'deleted'))
);

CREATE TABLE IF NOT EXISTS admin_role_change_logs (
    role_change_log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_email VARCHAR(255),
    change_description TEXT NOT NULL,
    modification_details JSONB DEFAULT '{}'::JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS deployment_audit_logs (
    deployment_audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_actor VARCHAR(255) DEFAULT 'system',
    release_description TEXT NOT NULL DEFAULT 'Automated production deployment',
    change_details TEXT,
    release_version VARCHAR(80),
    deployment_status VARCHAR(40) NOT NULL DEFAULT 'unknown',
    change_type VARCHAR(20) NOT NULL DEFAULT 'Both',
    source_commit VARCHAR(80),
    source_branch VARCHAR(120),
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT deployment_audit_status_check CHECK (deployment_status IN ('success', 'failed', 'in_progress', 'unknown')),
    CONSTRAINT deployment_audit_change_type_check CHECK (change_type IN ('FE', 'BE', 'Both', 'Infra'))
);

ALTER TABLE payment_orders
    ADD COLUMN IF NOT EXISTS support_contacted BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS support_comments VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS support_contacted_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS subscription_invoices (
    invoice_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number VARCHAR(80) UNIQUE NOT NULL,
    user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    profile_id UUID REFERENCES profiles(profile_id) ON DELETE SET NULL,
    advisor_id UUID REFERENCES advisors(advisor_id) ON DELETE SET NULL,
    subscription_id UUID REFERENCES subscriptions(subscription_id) ON DELETE SET NULL,
    transaction_id UUID REFERENCES transactions(transaction_id) ON DELETE SET NULL,
    plan_id VARCHAR(40),
    amount DECIMAL(10, 2) DEFAULT 0,
    currency VARCHAR(5) DEFAULT 'INR',
    status VARCHAR(24) DEFAULT 'issued',
    payment_method VARCHAR(40),
    transaction_ref VARCHAR(255),
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    issued_at TIMESTAMP DEFAULT NOW(),
    invoice_payload JSONB DEFAULT '{}'::JSONB
);

CREATE INDEX IF NOT EXISTS idx_admin_console_users_role_status ON admin_console_users(role, status);
CREATE INDEX IF NOT EXISTS idx_admin_role_change_logs_created ON admin_role_change_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_deployment_audit_logs_created ON deployment_audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_subscription_invoices_user ON subscription_invoices(user_id, issued_at DESC);
