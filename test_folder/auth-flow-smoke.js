const base = {
  auth: process.env.AUTH_BASE_URL || 'http://localhost:3001',
  profile: process.env.PROFILE_BASE_URL || 'http://localhost:3002'
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
  const phone = `+9198${String(Date.now()).slice(-8)}`;
  const results = [];

  const sendOtp = await request('send-otp', `${base.auth}/api/v1/auth/send-otp`, {
    method: 'POST',
    body: JSON.stringify({ phone })
  });
  results.push(sendOtp);
  expectStatus(sendOtp, 200);

  const verifyOtp = await request('verify-otp', `${base.auth}/api/v1/auth/verify-otp`, {
    method: 'POST',
    body: JSON.stringify({ phone, otp: process.env.MOCK_OTP_VALUE || '123456' })
  });
  results.push(verifyOtp);
  expectStatus(verifyOtp, 200);

  const auth = verifyOtp.body?.data;
  if (!auth?.accessToken || !auth?.refreshToken || !auth?.userId) {
    throw new Error(`verify-otp returned incomplete auth payload: ${JSON.stringify(verifyOtp.body)}`);
  }

  const authz = { authorization: `Bearer ${auth.accessToken}` };
  const myProfileBefore = await request('profile-me-before', `${base.profile}/api/v1/profile/me`, { headers: authz });
  results.push(myProfileBefore);
  expectStatus(myProfileBefore, 200);

  const step1 = await request('profile-step-1', `${base.profile}/api/v1/profile/create`, {
    method: 'POST',
    headers: authz,
    body: JSON.stringify({
      step: 1,
      firstName: 'Auth',
      lastName: 'Smoke',
      dob: '1995-01-15',
      gender: 'male',
      religion: 'Hindu',
      caste: 'Any',
      motherTongue: 'Hindi'
    })
  });
  results.push(step1);
  expectStatus(step1, 200);

  const myProfileAfter = await request('profile-me-after', `${base.profile}/api/v1/profile/me`, { headers: authz });
  results.push(myProfileAfter);
  expectStatus(myProfileAfter, 200);

  const refresh = await request('refresh-token', `${base.auth}/api/v1/auth/refresh-token`, {
    method: 'POST',
    body: JSON.stringify({ refreshToken: auth.refreshToken })
  });
  results.push(refresh);
  expectStatus(refresh, 200);

  const refreshedAuth = refresh.body?.data;
  if (!refreshedAuth?.accessToken || refreshedAuth.userId !== auth.userId || refreshedAuth.isNewUser !== false) {
    throw new Error(`refresh-token returned unexpected payload: ${JSON.stringify(refresh.body)}`);
  }

  const logout = await request('logout', `${base.auth}/api/v1/auth/logout`, {
    method: 'POST',
    body: JSON.stringify({ userId: auth.userId })
  });
  results.push(logout);
  expectStatus(logout, 200);

  console.log(JSON.stringify({ success: true, results }, null, 2));
}

main().catch(error => {
  console.error(JSON.stringify({ success: false, error: error.message }, null, 2));
  process.exit(1);
});
