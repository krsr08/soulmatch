CREATE TABLE IF NOT EXISTS outbox_events (
    outbox_event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(80) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID,
    payload JSONB NOT NULL DEFAULT '{}'::JSONB,
    status VARCHAR(24) NOT NULL DEFAULT 'pending',
    attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_error TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT outbox_events_status_check CHECK (status IN ('pending', 'retry', 'processing', 'completed', 'failed'))
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_ready
    ON outbox_events (status, available_at, created_at)
    WHERE status IN ('pending', 'retry');

CREATE TABLE IF NOT EXISTS notification_dlq (
    notification_dlq_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outbox_event_id UUID REFERENCES outbox_events(outbox_event_id) ON DELETE SET NULL,
    notification_id UUID REFERENCES notifications(notification_id) ON DELETE SET NULL,
    payload JSONB NOT NULL DEFAULT '{}'::JSONB,
    error TEXT,
    failed_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notification_dlq_failed_at
    ON notification_dlq (failed_at DESC);

CREATE TABLE IF NOT EXISTS notification_templates (
    template_key VARCHAR(120) PRIMARY KEY,
    title_template VARCHAR(160) NOT NULL,
    body_template TEXT NOT NULL,
    channel VARCHAR(32) DEFAULT 'push',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
