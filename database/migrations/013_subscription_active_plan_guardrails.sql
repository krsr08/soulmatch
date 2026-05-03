UPDATE subscriptions
SET is_active = false
WHERE is_active = true
  AND end_date IS NOT NULL
  AND end_date <= NOW();

WITH ranked AS (
    SELECT
        subscription_id,
        ROW_NUMBER() OVER (
            PARTITION BY user_id
            ORDER BY COALESCE(end_date, created_at) DESC, created_at DESC
        ) AS rn
    FROM subscriptions
    WHERE is_active = true
)
UPDATE subscriptions s
SET is_active = false
FROM ranked r
WHERE s.subscription_id = r.subscription_id
  AND r.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_subscriptions_one_active_per_user
ON subscriptions(user_id)
WHERE is_active = true;
