ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS gateway VARCHAR(40) DEFAULT 'razorpay',
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(40),
    ADD COLUMN IF NOT EXISTS payment_instrument VARCHAR(120),
    ADD COLUMN IF NOT EXISTS provider_status VARCHAR(40);

UPDATE transactions t
SET gateway = COALESCE(t.gateway, 'razorpay'),
    payment_method = COALESCE(
        t.payment_method,
        po.webhook_payload #>> '{payload,payment,entity,method}',
        po.webhook_payload #>> '{razorpayPayment,method}'
    ),
    payment_instrument = COALESCE(
        t.payment_instrument,
        po.webhook_payload #>> '{payload,payment,entity,vpa}',
        po.webhook_payload #>> '{razorpayPayment,vpa}'
    ),
    provider_status = COALESCE(
        t.provider_status,
        po.webhook_payload #>> '{payload,payment,entity,status}',
        po.webhook_payload #>> '{razorpayPayment,status}',
        po.status,
        t.status
    )
FROM payment_orders po
WHERE po.provider_order_id = t.razorpay_order_id
  AND po.user_id = t.user_id;
