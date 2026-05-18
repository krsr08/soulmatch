const assert = require('node:assert/strict');
const test = require('node:test');

const database = require('../src/config/database');
const controlPlane = require('../../../backend/shared/controlPlane');
const { deactivateExpiredSubscriptions } = require('../src/services/subscriptionExpiryService');

test('deactivateExpiredSubscriptions deactivates expired plans and records analytics', async () => {
  const calls = [];
  const analytics = [];
  const originalGetDB = database.getDB;
  const originalRecord = controlPlane.recordServerAnalyticsEvent;

  database.getDB = async () => ({
    query: async (sql) => {
      calls.push(sql);
      return {
        rowCount: 1,
        rows: [
          {
            subscription_id: 'sub-1',
            user_id: 'user-1',
            plan_id: 'gold',
            end_date: '2026-05-01T00:00:00.000Z',
          },
        ],
      };
    },
  });
  controlPlane.recordServerAnalyticsEvent = async (_db, event) => {
    analytics.push(event);
  };

  try {
    const expired = await deactivateExpiredSubscriptions();
    assert.equal(expired, 1);
    assert.match(calls[0], /UPDATE subscriptions/);
    assert.equal(analytics[0].eventType, 'subscription_expired');
    assert.equal(analytics[0].payload.planId, 'gold');
  } finally {
    database.getDB = originalGetDB;
    controlPlane.recordServerAnalyticsEvent = originalRecord;
  }
});
