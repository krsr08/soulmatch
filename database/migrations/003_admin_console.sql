ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(24) DEFAULT 'pending',
    ADD COLUMN IF NOT EXISTS admin_status VARCHAR(24) DEFAULT 'active',
    ADD COLUMN IF NOT EXISTS moderation_score NUMERIC(5,2) DEFAULT 0;

ALTER TABLE verifications
    ADD COLUMN IF NOT EXISTS reviewer_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS review_note TEXT;

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_email VARCHAR(255) NOT NULL,
    admin_role VARCHAR(80) NOT NULL,
    action VARCHAR(120) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(120),
    metadata JSONB DEFAULT '{}'::JSONB,
    ip_address VARCHAR(80),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS admin_alerts (
    alert_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    severity VARCHAR(20) DEFAULT 'medium',
    title VARCHAR(180) NOT NULL,
    body TEXT,
    source VARCHAR(80) DEFAULT 'admin-service',
    status VARCHAR(24) DEFAULT 'open',
    metadata JSONB DEFAULT '{}'::JSONB,
    acknowledged_by VARCHAR(255),
    acknowledged_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS admin_campaigns (
    campaign_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(180) NOT NULL,
    channel VARCHAR(40) NOT NULL,
    audience JSONB DEFAULT '{}'::JSONB,
    template_key VARCHAR(120),
    scheduled_at TIMESTAMP,
    status VARCHAR(24) DEFAULT 'draft',
    created_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_created ON admin_audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_alerts_status ON admin_alerts(status, severity);
CREATE INDEX IF NOT EXISTS idx_profiles_admin_status ON profiles(admin_status, verification_status);

INSERT INTO admin_alerts (severity, title, body, source, metadata)
SELECT
    'medium',
    'Admin console ready',
    'Real-time control plane monitoring is active.',
    'migration',
    '{"type":"bootstrap"}'::JSONB
WHERE NOT EXISTS (
    SELECT 1
    FROM admin_alerts
    WHERE source = 'migration'
      AND title = 'Admin console ready'
);
