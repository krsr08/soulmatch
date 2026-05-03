const Razorpay = require('razorpay');
const crypto = require('crypto');
const { randomUUID } = crypto;
const { getDB } = require('../config/database');
const logger = require('../utils/logger');
const { AppError, ErrorCodes } = require('../middleware/errorHandler');
const { getConfigSection, getPlanById, recordAnalyticsEvent } = require('../../shared/controlPlane');

const RENEWAL_WINDOW_DAYS = 7;

const MARK_PAYMENT_ORDER_PAID_SQL = `UPDATE payment_orders
       SET status='paid',
           provider_payment_id=$1,
           signature=COALESCE($2, signature),
           webhook_payload=CASE WHEN $3::jsonb = '{}'::jsonb THEN webhook_payload ELSE $3::jsonb END,
           metadata=COALESCE(metadata, '{}'::jsonb) || jsonb_build_object('activationSource', $4::text),
           updated_at=NOW()
       WHERE payment_order_id=$5`;

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

function paidUntilFromDays(days) {
  return new Date(Date.now() + days * 86400000).toISOString();
}

function planDisplayName(plan) {
  return plan?.name || plan?.planId || 'membership';
}

function daysUntil(dateValue) {
  if (!dateValue) return null;
  const expiryTime = new Date(dateValue).getTime();
  if (!Number.isFinite(expiryTime)) return null;
  return Math.ceil((expiryTime - Date.now()) / 86400000);
}

function planRank(monetization, plan) {
  if (!plan) return 0;
  const normalizedId = String(plan.planId || '').toLowerCase();
  const configuredIndex = (monetization?.plans || []).findIndex((configuredPlan) => {
    return String(configuredPlan.planId || '').toLowerCase() === normalizedId;
  });
  if (configuredIndex >= 0) return configuredIndex;
  const rankMap = { free: 0, silver: 1, gold: 2, platinum: 3 };
  if (Object.prototype.hasOwnProperty.call(rankMap, normalizedId)) return rankMap[normalizedId];
  return Math.max(1, Math.round(Number(plan.price || 0) / 500));
}

function buildPlanChangeContext(activeSubscription, targetPlan, monetization) {
  if (!activeSubscription || !activeSubscription.plan_id || activeSubscription.plan_id === 'free') {
    return {
      action: 'new',
      canCreateOrder: true,
      currentPlan: null,
      targetPlan,
      daysToExpiry: null,
      renewalWindowDays: RENEWAL_WINDOW_DAYS
    };
  }

  const currentPlan = getPlanById(monetization, activeSubscription.plan_id) || {
    planId: activeSubscription.plan_id,
    name: activeSubscription.plan_id,
    price: 0
  };
  const daysToExpiry = daysUntil(activeSubscription.end_date);
  const currentRank = planRank(monetization, currentPlan);
  const targetRank = planRank(monetization, targetPlan);
  const samePlan = String(currentPlan.planId) === String(targetPlan.planId);

  if (samePlan) {
    const canRenew = daysToExpiry !== null && daysToExpiry <= RENEWAL_WINDOW_DAYS;
    return {
      action: canRenew ? 'renew' : 'active_same_plan',
      canCreateOrder: canRenew,
      currentPlan,
      targetPlan,
      daysToExpiry,
      renewalWindowDays: RENEWAL_WINDOW_DAYS
    };
  }

  if (targetRank < currentRank || (targetRank === currentRank && Number(targetPlan.price || 0) < Number(currentPlan.price || 0))) {
    return {
      action: 'downgrade_blocked',
      canCreateOrder: false,
      currentPlan,
      targetPlan,
      daysToExpiry,
      renewalWindowDays: RENEWAL_WINDOW_DAYS
    };
  }

  return {
    action: 'upgrade',
    canCreateOrder: true,
    currentPlan,
    targetPlan,
    daysToExpiry,
    renewalWindowDays: RENEWAL_WINDOW_DAYS
  };
}

async function getCurrentActiveSubscription(db, userId) {
  const active = await db.query(
    `SELECT subscription_id, plan_id, start_date, end_date, is_active
     FROM subscriptions
     WHERE user_id=$1
       AND is_active=true
       AND (end_date IS NULL OR end_date>NOW())
     ORDER BY created_at DESC
     LIMIT 1`,
    [userId]
  );
  return active.rows[0] || null;
}

function assertPlanChangeAllowed(planContext) {
  if (planContext.canCreateOrder) return;
  if (planContext.action === 'downgrade_blocked') {
    throw new AppError(
      409,
      ErrorCodes.VALIDATION_ERROR,
      `Downgrade is not possible while your ${planDisplayName(planContext.currentPlan)} plan is active.`
    );
  }
  if (planContext.action === 'active_same_plan') {
    throw new AppError(
      409,
      ErrorCodes.VALIDATION_ERROR,
      `Your ${planDisplayName(planContext.currentPlan)} plan is still active. Renewal opens during the last ${RENEWAL_WINDOW_DAYS} days.`
    );
  }
}

function summarizeRazorpayPayment(payment = {}) {
  const method = String(payment.method || '').toLowerCase() || null;
  const card = payment.card || {};
  let instrument = null;
  if (method === 'card') {
    const parts = [card.network, card.type, card.last4 ? `ending ${card.last4}` : null].filter(Boolean);
    instrument = parts.length ? parts.join(' ') : 'Card';
  } else if (method === 'upi') {
    instrument = payment.vpa || payment.upi?.vpa || 'UPI';
  } else if (method === 'netbanking') {
    instrument = payment.bank || 'Netbanking';
  } else if (method === 'wallet') {
    instrument = payment.wallet || 'Wallet';
  } else if (method === 'emi') {
    instrument = payment.emi?.bank || 'EMI';
  } else if (method) {
    instrument = method;
  }

  return {
    gateway: 'razorpay',
    paymentMethod: method,
    paymentInstrument: instrument,
    providerStatus: payment.status || null
  };
}

async function fetchRazorpayPaymentDetails(paymentId) {
  try {
    return await getRazorpay().payments.fetch(paymentId, { 'expand[]': 'card' });
  } catch (error) {
    logger.error(`Unable to fetch Razorpay payment details ${paymentId}: ${error.message}`);
    return null;
  }
}

async function activatePaidOrder(db, { order, plan, paymentId, signature = null, webhookPayload = null, source = 'client_verify', paymentDetails = null }) {
  if (!order) {
    throw new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Payment order was not found.');
  }
  if (!paymentId) {
    throw new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Razorpay payment id is required to activate membership.');
  }

  const days = parseInt(plan.durationDays || 30, 10) || 30;
  const paymentSummary = summarizeRazorpayPayment(paymentDetails || webhookPayload?.payload?.payment?.entity || webhookPayload?.razorpayPayment || {});
  const client = await db.connect();
  const subId = randomUUID();
  try {
    await client.query('BEGIN');
    const locked = await client.query('SELECT * FROM payment_orders WHERE payment_order_id=$1 FOR UPDATE', [order.payment_order_id]);
    const currentOrder = locked.rows[0];
    if (!currentOrder) {
      throw new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Payment order was not found.');
    }

    if (currentOrder.status === 'paid') {
      const existing = await client.query(
        `SELECT subscription_id, plan_id, end_date
         FROM subscriptions
         WHERE payment_id=$1 AND user_id=$2
         ORDER BY created_at DESC
         LIMIT 1`,
        [currentOrder.provider_payment_id || paymentId, currentOrder.user_id]
      );
      await client.query('COMMIT');
      return {
        subscriptionId: existing.rows[0]?.subscription_id,
        planId: existing.rows[0]?.plan_id || plan.planId,
        planName: plan.name,
        activeTill: existing.rows[0]?.end_date
      };
    }

    const activeSubscription = await getCurrentActiveSubscription(client, currentOrder.user_id);
    const planChangeContext = buildPlanChangeContext(activeSubscription, plan, null);
    assertPlanChangeAllowed(planChangeContext);
    await client.query('UPDATE subscriptions SET is_active=false WHERE user_id=$1 AND is_active=true', [currentOrder.user_id]);
    await client.query(
      `INSERT INTO subscriptions (subscription_id,user_id,plan_id,end_date,is_active,payment_id,amount_paid)
       VALUES ($1,$2,$3,NOW() + ($4::text || ' days')::interval,true,$5,$6)`,
      [subId, currentOrder.user_id, plan.planId, String(days), paymentId, currentOrder.amount]
    );
    await client.query(
      `INSERT INTO transactions (
         transaction_id,user_id,subscription_id,razorpay_order_id,razorpay_payment_id,amount,currency,status,
         gateway,payment_method,payment_instrument,provider_status
       )
       VALUES ($1,$2,$3,$4,$5,$6,$7,'success',$8,$9,$10,$11)`,
      [
        randomUUID(),
        currentOrder.user_id,
        subId,
        currentOrder.provider_order_id,
        paymentId,
        currentOrder.amount,
        currentOrder.currency,
        paymentSummary.gateway,
        paymentSummary.paymentMethod,
        paymentSummary.paymentInstrument,
        paymentSummary.providerStatus
      ]
    );
    await client.query(
      MARK_PAYMENT_ORDER_PAID_SQL,
      [paymentId, signature, JSON.stringify(webhookPayload || {}), source, currentOrder.payment_order_id]
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
    userId: order.user_id,
    payload: { planId: plan.planId, gateway: 'razorpay', amount: Number(order.amount), subscriptionId: subId, source }
  });

  return {
    subscriptionId: subId,
    planId: plan.planId,
    planName: plan.name,
    activeTill: paidUntilFromDays(days)
  };
}

async function loadRuntime(db) {
  const [monetization, gateways] = await Promise.all([
    getConfigSection(db, 'monetization'),
    getConfigSection(db, 'payment_gateways')
  ]);
  return { monetization, gateways };
}

function findCapturedPaymentForOrder(paymentsPayload, order) {
  const items = Array.isArray(paymentsPayload?.items) ? paymentsPayload.items : [];
  const expectedAmount = Math.round(Number(order.amount) * 100);
  return items.find((payment) => {
    return payment &&
      payment.status === 'captured' &&
      payment.order_id === order.provider_order_id &&
      Number(payment.amount) === expectedAmount &&
      String(payment.currency || '').toUpperCase() === String(order.currency || 'INR').toUpperCase();
  }) || null;
}

async function reconcileUserPaidOrders(db, userId, monetization) {
  const pendingOrders = await db.query(
    `SELECT *
     FROM payment_orders
     WHERE user_id=$1
       AND status IN ('created','captured','updated')
     ORDER BY created_at DESC
     LIMIT 5`,
    [userId]
  );

  for (const order of pendingOrders.rows) {
    try {
      const plan = getPlanById(monetization, order.plan_id);
      if (!plan) continue;
      const paymentsPayload = await getRazorpay().orders.fetchPayments(order.provider_order_id);
      const payment = findCapturedPaymentForOrder(paymentsPayload, order);
      if (!payment?.id) continue;
      await activatePaidOrder(db, {
        order,
        plan,
        paymentId: payment.id,
        webhookPayload: {
          reconciliation: true,
          razorpayPayment: {
            id: payment.id,
            order_id: payment.order_id,
            status: payment.status,
            amount: payment.amount,
            currency: payment.currency,
            method: payment.method
          }
        },
        source: 'subscription_reconcile',
        paymentDetails: payment
      });
      logger.info(`Reconciled paid Razorpay order ${order.provider_order_id} for user ${userId}`);
    } catch (error) {
      logger.error(`Payment reconciliation failed for order ${order.provider_order_id}: ${error.message}`);
    }
  }
}

async function enrichUserTransactionHistory(db, userId) {
  const missingDetails = await db.query(
    `SELECT transaction_id, razorpay_payment_id
     FROM transactions
     WHERE user_id=$1
       AND razorpay_payment_id IS NOT NULL
       AND (payment_method IS NULL OR payment_instrument IS NULL OR provider_status IS NULL)
     ORDER BY created_at DESC
     LIMIT 10`,
    [userId]
  );

  for (const row of missingDetails.rows) {
    const paymentDetails = await fetchRazorpayPaymentDetails(row.razorpay_payment_id);
    if (!paymentDetails) continue;
    const summary = summarizeRazorpayPayment(paymentDetails);
    try {
      await db.query(
        `UPDATE transactions
         SET gateway=COALESCE(gateway, $1),
             payment_method=COALESCE(payment_method, $2),
             payment_instrument=COALESCE(payment_instrument, $3),
             provider_status=COALESCE(provider_status, $4)
         WHERE transaction_id=$5`,
        [summary.gateway, summary.paymentMethod, summary.paymentInstrument, summary.providerStatus, row.transaction_id]
      );
    } catch (error) {
      logger.error(`Payment history enrichment failed for transaction ${row.transaction_id}: ${error.message}`);
    }
  }
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
    const activeSubscription = await getCurrentActiveSubscription(db, req.user.userId);
    const planChangeContext = buildPlanChangeContext(activeSubscription, plan, monetization);
    assertPlanChangeAllowed(planChangeContext);
    const amount = Number(plan.price);
    const currency = monetization.currency || 'INR';
    const receipt = 'rcpt_' + randomUUID().replace(/-/g, '').slice(0, 24);
    const order = await getRazorpay().orders.create({
      amount:amount*100,
      currency,
      receipt,
      notes:{ planId: plan.planId, requestedPlanId: planId, userId:req.user.userId, purchaseAction: planChangeContext.action }
    });
    await db.query(
      `INSERT INTO payment_orders (payment_order_id,user_id,plan_id,amount,currency,gateway,provider_order_id,status,metadata)
       VALUES ($1,$2,$3,$4,$5,'razorpay',$6,'created',$7::jsonb)`,
      [randomUUID(), req.user.userId, plan.planId, amount, currency, order.id, JSON.stringify({ receipt, gatewayOrder: order, requestedPlanId: planId, purchaseAction: planChangeContext.action })]
    );
    await recordAnalyticsEvent(db, {
      eventType: 'payment_click',
      serviceName: 'payment-service',
      userId: req.user.userId,
      payload: { planId: plan.planId, requestedPlanId: planId, gateway: 'razorpay', amount: plan.price, purchaseAction: planChangeContext.action }
    });
    res.json({ success:true, data:{ orderId:order.id, amount:order.amount, currency:order.currency, planId:plan.planId, gateway:'razorpay', keyId:getRazorpayCredentials().key_id } });
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
    if (String(order.plan_id) !== String(plan.planId) || Number(order.amount) !== Number(plan.price) || String(order.currency) !== String(monetization.currency || 'INR')) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Payment order details do not match the selected plan.'));
    }
    const paymentDetails = await fetchRazorpayPaymentDetails(paymentId);
    if (paymentDetails?.order_id && paymentDetails.order_id !== orderId) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Payment does not belong to this SoulMatch order.'));
    }
    if (paymentDetails?.status && !['captured', 'authorized'].includes(paymentDetails.status)) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Razorpay has not confirmed this payment yet.'));
    }
    const activation = await activatePaidOrder(db, { order, plan, paymentId, signature, source: 'client_verify', paymentDetails });
    logger.info('Payment verified for user '+userId+' plan '+plan.planId);
    res.json({ success:true, data: activation });
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
      const orderResult = await db.query('SELECT * FROM payment_orders WHERE provider_order_id=$1 LIMIT 1', [orderId]);
      const paymentOrder = orderResult.rows[0];
      const paidEvents = new Set(['payment.captured', 'order.paid']);
      if (paymentOrder && paidEvents.has(event.event)) {
        const { monetization } = await loadRuntime(db);
        const plan = getPlanById(monetization, paymentOrder.plan_id);
        if (plan && paymentId) {
          await activatePaidOrder(db, {
            order: paymentOrder,
            plan,
            paymentId,
            webhookPayload: event,
            source: 'razorpay_webhook'
          });
        } else {
          await db.query(
            `UPDATE payment_orders
             SET status='captured', provider_payment_id=COALESCE($1, provider_payment_id), webhook_payload=$2::jsonb, updated_at=NOW()
             WHERE provider_order_id=$3`,
            [paymentId, JSON.stringify(event), orderId]
          );
        }
      } else {
        const status = event.event === 'payment.failed' ? 'failed' : 'updated';
        await db.query(
          `UPDATE payment_orders
           SET status=$1, provider_payment_id=COALESCE($2, provider_payment_id), webhook_payload=$3::jsonb, updated_at=NOW()
           WHERE provider_order_id=$4`,
          [status, paymentId, JSON.stringify(event), orderId]
        );
      }
    }
    res.json({ success:true });
  } catch (err) { next(err); }
};
exports.getSubscription = async (req, res, next) => {
  try {
    const db = await getDB();
    const activeSubscriptionSql = "SELECT plan_id,start_date,end_date,is_active FROM subscriptions WHERE user_id=$1 AND is_active=true AND (end_date IS NULL OR end_date>NOW()) ORDER BY created_at DESC LIMIT 1";
    let r = await db.query(activeSubscriptionSql, [req.user.userId]);
    if (!r.rows[0] || r.rows[0].plan_id === 'free') {
      const { monetization } = await loadRuntime(db);
      await reconcileUserPaidOrders(db, req.user.userId, monetization);
      r = await db.query(activeSubscriptionSql, [req.user.userId]);
    }
    res.json({ success:true, data:r.rows[0]||{ plan_id:'free', is_active:true } });
  } catch (err) { next(err); }
};
exports.getInvoices = async (req, res, next) => {
  try {
    const db = await getDB();
    const { monetization } = await loadRuntime(db);
    await enrichUserTransactionHistory(db, req.user.userId);
    const r = await db.query(
      `SELECT
         t.transaction_id,
         t.created_at,
         t.amount,
         t.currency,
         t.status,
         t.gateway,
         COALESCE(t.payment_method, po.webhook_payload #>> '{payload,payment,entity,method}', po.webhook_payload #>> '{razorpayPayment,method}') AS payment_method,
         COALESCE(t.payment_instrument, po.webhook_payload #>> '{payload,payment,entity,vpa}', po.webhook_payload #>> '{razorpayPayment,vpa}') AS payment_instrument,
         COALESCE(t.provider_status, po.webhook_payload #>> '{payload,payment,entity,status}', po.webhook_payload #>> '{razorpayPayment,status}', po.status, t.status) AS provider_status,
         t.razorpay_order_id,
         t.razorpay_payment_id,
         po.payment_order_id,
         po.status AS payment_order_status,
         s.subscription_id,
         s.plan_id,
         s.start_date,
         s.end_date,
         s.is_active
       FROM transactions t
       JOIN subscriptions s ON t.subscription_id=s.subscription_id
       LEFT JOIN payment_orders po
         ON po.provider_order_id=t.razorpay_order_id
        AND po.user_id=t.user_id
       WHERE t.user_id=$1
       ORDER BY t.created_at DESC`,
      [req.user.userId]
    );
    const data = r.rows.map((row) => {
      const plan = getPlanById(monetization, row.plan_id);
      return {
        ...row,
        plan_name: plan?.name || row.plan_id,
        duration_days: plan?.durationDays || null
      };
    });
    res.json({ success:true, data });
  } catch (err) { next(err); }
};

exports._test = {
  MARK_PAYMENT_ORDER_PAID_SQL,
  buildPlanChangeContext,
  findCapturedPaymentForOrder,
  planRank,
  summarizeRazorpayPayment
};
