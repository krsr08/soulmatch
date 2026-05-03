const assert = require('node:assert/strict');
const test = require('node:test');

const { _test } = require('../src/controllers/paymentController');

test('payment order activation SQL casts activation source for jsonb_build_object', () => {
  assert.match(_test.MARK_PAYMENT_ORDER_PAID_SQL, /jsonb_build_object\('activationSource', \$4::text\)/);
});

test('findCapturedPaymentForOrder returns the matching captured Razorpay payment', () => {
  const order = {
    provider_order_id: 'order_123',
    amount: '999.00',
    currency: 'INR'
  };
  const payment = _test.findCapturedPaymentForOrder(
    {
      items: [
        { id: 'pay_wrong', order_id: 'order_123', status: 'failed', amount: 99900, currency: 'INR' },
        { id: 'pay_other', order_id: 'order_other', status: 'captured', amount: 99900, currency: 'INR' },
        { id: 'pay_ok', order_id: 'order_123', status: 'captured', amount: 99900, currency: 'INR', method: 'card' }
      ]
    },
    order
  );
  assert.equal(payment.id, 'pay_ok');
});

test('findCapturedPaymentForOrder rejects mismatched amount or currency', () => {
  const order = {
    provider_order_id: 'order_123',
    amount: '999.00',
    currency: 'INR'
  };
  assert.equal(
    _test.findCapturedPaymentForOrder(
      { items: [{ id: 'pay_usd', order_id: 'order_123', status: 'captured', amount: 99900, currency: 'USD' }] },
      order
    ),
    null
  );
  assert.equal(
    _test.findCapturedPaymentForOrder(
      { items: [{ id: 'pay_amount', order_id: 'order_123', status: 'captured', amount: 49900, currency: 'INR' }] },
      order
    ),
    null
  );
});

test('summarizeRazorpayPayment keeps safe card and UPI display details', () => {
  assert.deepEqual(
    _test.summarizeRazorpayPayment({
      method: 'card',
      status: 'captured',
      card: { network: 'Visa', type: 'credit', last4: '1007' }
    }),
    {
      gateway: 'razorpay',
      paymentMethod: 'card',
      paymentInstrument: 'Visa credit ending 1007',
      providerStatus: 'captured'
    }
  );
  assert.deepEqual(
    _test.summarizeRazorpayPayment({ method: 'upi', status: 'captured', vpa: 'success@razorpay' }),
    {
      gateway: 'razorpay',
      paymentMethod: 'upi',
      paymentInstrument: 'success@razorpay',
      providerStatus: 'captured'
    }
  );
});
