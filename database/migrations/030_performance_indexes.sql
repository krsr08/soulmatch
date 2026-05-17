CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_profiles_discovery_feed
    ON profiles (is_published, admin_status, profile_status, gender, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_profiles_user_id
    ON profiles (user_id);

CREATE INDEX IF NOT EXISTS idx_profiles_updated_at
    ON profiles (updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_profiles_published_created
    ON profiles (is_published, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_profiles_search_text_trgm
    ON profiles
    USING gin (
        (
            COALESCE(first_name, '') || ' ' ||
            COALESCE(last_name, '') || ' ' ||
            COALESCE(religion, '') || ' ' ||
            COALESCE(caste, '') || ' ' ||
            COALESCE(mother_tongue, '')
        ) gin_trgm_ops
    );

CREATE INDEX IF NOT EXISTS idx_blocks_blocker_blocked
    ON blocks (blocker_id, blocked_id);

CREATE INDEX IF NOT EXISTS idx_blocks_blocked_blocker
    ON blocks (blocked_id, blocker_id);

CREATE INDEX IF NOT EXISTS idx_interests_sender_receiver_status
    ON interests (sender_id, receiver_id, status);

CREATE INDEX IF NOT EXISTS idx_interests_receiver_status_sent
    ON interests (receiver_id, status, sent_at DESC);

CREATE INDEX IF NOT EXISTS idx_reports_reported_status_created
    ON reports (reported_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_verifications_user_status_created
    ON verifications (user_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payment_orders_user_status_created
    ON payment_orders (user_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payment_orders_provider_status
    ON payment_orders (provider_order_id, status);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_active_end
    ON subscriptions (user_id, is_active, end_date DESC);

CREATE INDEX IF NOT EXISTS idx_profile_photos_profile_approved_order
    ON profile_photos (profile_id, is_approved, sequence_order);

CREATE INDEX IF NOT EXISTS idx_profile_photo_access_target_requester
    ON profile_photo_access_requests (target_profile_id, requester_user_id, status, expires_at);

CREATE INDEX IF NOT EXISTS idx_profile_views_viewer_viewed_at
    ON profile_views (viewer_id, viewed_at DESC);

CREATE INDEX IF NOT EXISTS idx_match_feedback_user_action_created
    ON match_feedback (user_id, action, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_analytics_events_type_created
    ON analytics_events (event_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_member_meter_events_target_period
    ON member_meter_events (target_profile_id, event_type, period_key);
