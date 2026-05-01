const Razorpay = require('razorpay');
const crypto = require('crypto');
const { randomUUID } = crypto;
const { getDB } = require('../config/database');
const logger = require('../utils/logger');
const { AppError, ErrorCodes } = require('../middleware/errorHandler');
const { getConfigSection, getPlanById, recordAnalyticsEvent } = require('../../shared/controlPlane');

function getRazorpayCredentials() {
  const keyId = process.env.RAZORPAY_KEY_ID;
  const keySecret = process.env.RAZORPAY_KEY_SECRET;
  if ((!keyId || !keySecret) && process.env.NODE_ENV === 'production') {
    throw new AppError(503, ErrorCodes.SERVICE_UNAVAILABLE, 'Payment gateway is not configured.');
  }
  return {
    key_id: keyId || 'rzp_test_placeholder',
    key_secret: keySecret || 'placeholder'
  };
}

const getRazorpay = () => new Razorpay(getRazorpayCredentials());

async function loadRuntime(db) {
  const [monetization, gateways] = await Promise.all([
    getConfigSection(db, 'monetization'),
    getConfigSection(db, 'payment_gateways')
  ]);
  return { monetization, gateways };
}

exports.getPlans = async (req, res, next) => {
  try {
    const db = await getDB();
    const { monetization } = await loadRuntime(db);
    res.json({ success: true, data: monetization.plans || [] });
  } catch (err) {
    next(err);
  }
};

exports.getUpgradePackages = async (req, res, next) => {
  try {
    const db = await getDB();
    const { monetization } = await loadRuntime(db);
    res.json({ success: true, data: monetization.upgradePackageGroups || [] });
  } catch (err) {
    next(err);
  }
};

exports.createOrder = async (req, res, next) => {
  try {
    const { planId } = req.body || {};
    if (!planId) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Select a subscription plan to continue.'));
    }
    const db = await getDB();
    const { monetization, gateways } = await loadRuntime(db);
    const gateway = gateways.razorpay || { enabled: true };
    if (!gateway.enabled) {
      return next(new AppError(503, ErrorCodes.SERVICE_UNAVAILABLE, 'Payments are temporarily unavailable. Please try again shortly.'));
    }
    const plan = getPlanById(monetization, planId);
    if (!plan) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'The selected subscription plan is unavailable.'));
    }
    if (plan.price === 0) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'The free plan does not require a payment order.'));
    }
    const amount = Number(plan.price);
    const currency = monetization.currency || 'INR';
    const receipt = 'rcpt_' + randomUUID().replace(/-/g, '').slice(0, 24);
    const order = await getRazorpay().orders.create({ amount:amount*100, currency, receipt, notes:{ planId, userId:req.user.userId } });
    await db.query(
      `INSERT INTO payment_orders (payment_order_id,user_id,plan_id,amount,currency,gateway,provider_order_id,status,metadata)
       VALUES ($1,$2,$3,$4,$5,'razorpay',$6,'created',$7::jsonb)`,
      [randomUUID(), req.user.userId, planId, amount, currency, order.id, JSON.stringify({ receipt, gatewayOrder: order })]
    );
    await recordAnalyticsEvent(db, {
      eventType: 'payment_click',
      serviceName: 'payment-service',
      userId: req.user.userId,
      payload: { planId, gateway: 'razorpay', amount: plan.price }
    });
    res.json({ success:true, data:{ orderId:order.id, amount:order.amount, currency:order.currency, planId, gateway:'razorpay' } });
  } catch (err) { next(err); }
};
exports.verifyPayment = async (req, res, next) => {
  try {
    const { orderId, paymentId, signature, planId } = req.body;
    const userId = req.user.userId;
    if (!orderId || !paymentId || !signature || !planId) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'orderId, paymentId, signature, and planId are required.'));
    }
    const expectedSig = crypto.createHmac('sha256', getRazorpayCredentials().key_secret).update(orderId+'|'+paymentId).digest('hex');
    if (expectedSig !== signature) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Payment verification failed. Please retry the payment confirmation.'));
    }
    const db = await getDB();
    const { monetization, gateways } = await loadRuntime(db);
    const gateway = gateways.razorpay || { enabled: true };
    if (!gateway.enabled) {
      return next(new AppError(503, ErrorCodes.SERVICE_UNAVAILABLE, 'Payments are temporarily unavailable. Please try again shortly.'));
    }
    const plan = getPlanById(monetization, planId);
    if (!plan) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'The selected subscription plan is unavailable.'));
    }
    const paymentOrder = await db.query(
      `SELECT * FROM payment_orders
       WHERE provider_order_id=$1 AND user_id=$2
       LIMIT 1`,
      [orderId, userId]
    );
    const order = paymentOrder.rows[0];
    if (!order) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Payment order was not created by SoulMatch.'));
    }
    if (order.status === 'paid') {
      const existing = await db.query(
        "SELECT subscription_id, plan_id, end_date FROM subscriptions WHERE payment_id=$1 AND user_id=$2 ORDER BY created_at DESC LIMIT 1",
        [paymentId, userId]
      );
      return res.json({ success:true, data:{ subscriptionId: existing.rows[0]?.subscription_id, planId: existing.rows[0]?.plan_id || planId, planName: plan.name, activeTill: existing.rows[0]?.end_date } });
    }
    if (String(order.plan_id) !== String(planId) || Number(order.amount) !== Number(plan.price) || String(order.currency) !== String(monetization.currency || 'INR')) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Payment order details do not match the selected plan.'));
    }
    const days = parseInt(plan.durationDays || 30, 10) || 30;
    const client = await db.connect();
    const subId = randomUUID();
    try {
      await client.query('BEGIN');
      await client.query('UPDATE subscriptions SET is_active=false WHERE user_id=$1 AND is_active=true', [userId]);
      await client.query(
        `INSERT INTO subscriptions (subscription_id,user_id,plan_id,end_date,is_active,payment_id,amount_paid)
         VALUES ($1,$2,$3,NOW() + ($4::text || ' days')::interval,true,$5,$6)`,
        [subId, userId, planId, String(days), paymentId, plan.price]
      );
      await client.query(
        "INSERT INTO transactions (transaction_id,user_id,subscription_id,razorpay_order_id,razorpay_payment_id,amount,currency,status) VALUES ($1,$2,$3,$4,$5,$6,$7,'success')",
        [randomUUID(), userId, subId, orderId, paymentId, plan.price, monetization.currency || 'INR']
      );
      await client.query(
        "UPDATE payment_orders SET status='paid', provider_payment_id=$1, signature=$2, updated_at=NOW() WHERE provider_order_id=$3 AND user_id=$4",
        [paymentId, signature, orderId, userId]
      );
      await client.query('COMMIT');
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
    await recordAnalyticsEvent(db, {
      eventType: 'payment_success',
      serviceName: 'payment-service',
      userId,
      payload: { planId, gateway: 'razorpay', amount: plan.price, subscriptionId: subId }
    });
    logger.info('Payment verified for user '+userId+' plan '+planId);
    res.json({ success:true, data:{ subscriptionId:subId, planId, planName:plan.name, activeTill:new Date(Date.now()+days*86400000).toISOString() } });
  } catch (err) { next(err); }
};
exports.handleWebhook = async (req, res, next) => {
  try {
    const secret = process.env.RAZORPAY_WEBHOOK_SECRET;
    if (!secret) {
      return res.status(503).json({ success:false, error:{ code:'SERVICE_UNAVAILABLE', message:'Webhook secret is not configured.' } });
    }
    const signature = req.headers['x-razorpay-signature'];
    const rawBody = Buffer.isBuffer(req.body) ? req.body : Buffer.from(req.body || '');
    const expected = crypto.createHmac('sha256', secret).update(rawBody).digest('hex');
    if (!signature || expected !== signature) {
      return res.status(400).json({ success:false, error:{ code:'VALIDATION_ERROR', message:'Invalid webhook signature.' } });
    }
    const event = JSON.parse(rawBody.toString('utf8'));
    const db = await getDB();
    const orderId = event?.payload?.payment?.entity?.order_id || event?.payload?.order?.entity?.id;
    const paymentId = event?.payload?.payment?.entity?.id || null;
    if (orderId) {
      const status = event.event === 'payment.failed' ? 'failed' : event.event === 'payment.captured' ? 'captured' : 'updated';
      await db.query(
        `UPDATE payment_orders
         SET status=$1, provider_payment_id=COALESCE($2, provider_payment_id), webhook_payload=$3::jsonb, updated_at=NOW()
         WHERE provider_order_id=$4`,
        [status, paymentId, JSON.stringify(event), orderId]
      );
    }
    res.json({ success:true });
  } catch (err) { next(err); }
};
exports.getSubscription = async (req, res, next) => {
  try {
    const db = await getDB();
    const r = await db.query("SELECT plan_id,start_date,end_date,is_active FROM subscriptions WHERE user_id=$1 AND is_active=true AND (end_date IS NULL OR end_date>NOW()) ORDER BY created_at DESC LIMIT 1", [req.user.userId]);
    res.json({ success:true, data:r.rows[0]||{ plan_id:'free', is_active:true } });
  } catch (err) { next(err); }
};
exports.getInvoices = async (req, res, next) => {
  try {
    const db = await getDB();
    const r = await db.query("SELECT t.transaction_id,t.created_at,t.amount,s.plan_id FROM transactions t JOIN subscriptions s ON t.subscription_id=s.subscription_id WHERE t.user_id=$1 AND t.status='success' ORDER BY t.created_at DESC", [req.user.userId]);
    res.json({ success:true, data:r.rows });
  } catch (err) { next(err); }
};
