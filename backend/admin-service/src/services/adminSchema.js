const { getDB } = require('../config/database');
const logger = require('../utils/logger');

async function ensureAdminSchema() {
  try {
    const db = await getDB();
    await db.query(`
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
        updated_at TIMESTAMP DEFAULT NOW()
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
        created_at TIMESTAMP DEFAULT NOW()
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

      CREATE INDEX IF NOT EXISTS idx_admin_audit_created ON admin_audit_logs(created_at DESC);
      CREATE INDEX IF NOT EXISTS idx_admin_alerts_status ON admin_alerts(status, severity);
      CREATE INDEX IF NOT EXISTS idx_profiles_admin_status ON profiles(admin_status, verification_status);
      CREATE INDEX IF NOT EXISTS idx_admin_console_users_role_status ON admin_console_users(role, status);
      CREATE INDEX IF NOT EXISTS idx_admin_role_change_logs_created ON admin_role_change_logs(created_at DESC);
      CREATE INDEX IF NOT EXISTS idx_deployment_audit_logs_created ON deployment_audit_logs(created_at DESC);
      CREATE INDEX IF NOT EXISTS idx_subscription_invoices_user ON subscription_invoices(user_id, issued_at DESC);
    `);
  } catch (error) {
    logger.warn(`Admin schema bootstrap skipped: ${error.message}`);
  }
}

module.exports = { ensureAdminSchema };
