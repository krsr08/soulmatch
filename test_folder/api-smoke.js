const base = {
  admin: process.env.ADMIN_BASE_URL || 'http://localhost:3011',
  auth: process.env.AUTH_BASE_URL || 'http://localhost:3001',
  profile: process.env.PROFILE_BASE_URL || 'http://localhost:3002',
  search: process.env.SEARCH_BASE_URL || 'http://localhost:3004',
  chat: process.env.CHAT_BASE_URL || 'http://localhost:3005',
  notification: process.env.NOTIFICATION_BASE_URL || 'http://localhost:3006',
  payment: process.env.PAYMENT_BASE_URL || 'http://localhost:3007'
};

async function request(name, url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: {
      'content-type': 'application/json',
      ...(options.headers || {})
    }
  });
  const text = await response.text();
  let body;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    body = text;
  }
  return { name, status: response.status, body };
}

function expectStatus(result, expectedStatus) {
  if (result.status !== expectedStatus) {
    throw new Error(`${result.name} expected ${expectedStatus} but received ${result.status}: ${JSON.stringify(result.body)}`);
  }
}

async function main() {
  const phone = `+9199${String(Date.now()).slice(-8)}`;
  const results = [];

  results.push(await request('admin-health', `${base.admin}/health`));
  results.push(await request('public-config', `${base.admin}/api/v1/public/config`));
  results.push(await request('payment-plans', `${base.payment}/api/v1/payment/plans`));
  results.push(await request('send-otp-invalid-phone', `${base.auth}/api/v1/auth/send-otp`, {
    method: 'POST',
    body: JSON.stringify({ phone: '123' })
  }));
  results.push(await request('send-otp', `${base.auth}/api/v1/auth/send-otp`, {
    method: 'POST',
    body: JSON.stringify({ phone })
  }));
  results.push(await request('verify-otp-invalid', `${base.auth}/api/v1/auth/verify-otp`, {
    method: 'POST',
    body: JSON.stringify({ phone, otp: '000000' })
  }));

  const verify = await request('verify-otp-valid', `${base.auth}/api/v1/auth/verify-otp`, {
    method: 'POST',
    body: JSON.stringify({ phone, otp: process.env.MOCK_OTP_VALUE || '123456' })
  });
  results.push(verify);

  expectStatus(results[0], 200);
  expectStatus(results[1], 200);
  expectStatus(results[2], 200);
  expectStatus(results[3], 400);
  expectStatus(results[4], 200);
  expectStatus(results[5], 400);
  expectStatus(results[6], 200);

  const token = verify.body?.data?.accessToken;
  if (!token) {
    throw new Error('verify-otp-valid did not return an access token');
  }

  const authz = { authorization: `Bearer ${token}` };
  const protectedResults = [
    await request('profile-step-1', `${base.profile}/api/v1/profile/create`, {
      method: 'POST',
      headers: authz,
      body: JSON.stringify({
        step: 1,
        firstName: 'Smoke',
        lastName: 'Tester',
        dob: '1994-06-15',
        gender: 'male',
        religion: 'Hindu',
        caste: 'Any',
        motherTongue: 'Hindi'
      })
    }),
    await request('search-basic', `${base.search}/api/v1/search/basic`, {
      method: 'POST',
      headers: authz,
      body: JSON.stringify({ page: 1 })
    }),
    await request('chat-conversations', `${base.chat}/api/v1/chat/conversations`, {
      headers: authz
    }),
    await request('payment-create-order-missing-plan', `${base.payment}/api/v1/payment/create-order`, {
      method: 'POST',
      headers: authz,
      body: JSON.stringify({})
    }),
    await request('notification-template-missing-fields', `${base.notification}/api/v1/notifications/template`, {
      method: 'POST',
      body: JSON.stringify({})
    })
  ];

  protectedResults.forEach(result => results.push(result));

  expectStatus(protectedResults[0], 200);
  expectStatus(protectedResults[1], 200);
  expectStatus(protectedResults[2], 200);
  expectStatus(protectedResults[3], 400);
  expectStatus(protectedResults[4], 400);

  console.log(JSON.stringify({ success: true, results }, null, 2));
}

main().catch(error => {
  console.error(JSON.stringify({
    success: false,
    error: error.message
  }, null, 2));
  process.exit(1);
});
