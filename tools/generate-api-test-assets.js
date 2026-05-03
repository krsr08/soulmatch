const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const outDir = path.join(root, 'api-testing');
const k6Dir = path.join(outDir, 'k6');

const rawJson = (value) => ({
  mode: 'raw',
  raw: JSON.stringify(value, null, 2),
  options: { raw: { language: 'json' } }
});

const bearer = (token = '{{access_token}}') => ({
  type: 'bearer',
  bearer: [{ key: 'token', value: token, type: 'string' }]
});

const noAuth = { type: 'noauth' };

const jsonHeader = { key: 'Content-Type', value: 'application/json' };

const request = ({
  name,
  method,
  path,
  body,
  auth,
  headers = [],
  query = [],
  tests = '',
  description = ''
}) => ({
  name,
  event: tests ? [{ listen: 'test', script: { type: 'text/javascript', exec: tests.split('\n') } }] : [],
  request: {
    auth,
    method,
    header: body && body.mode === 'raw' ? [jsonHeader, ...headers] : headers,
    body,
    url: {
      raw: path.startsWith('http') ? path : `{{base_url}}${path}`,
      host: path.startsWith('http') ? [path] : ['{{base_url}}'],
      path: path.startsWith('http') ? [] : path.replace(/^\//, '').split('/'),
      query
    },
    description
  }
});

const folder = (name, items) => ({ name, item: items });

const setAuthTests = `const json = pm.response.json();
pm.test("login succeeded", function () { pm.expect(json.success).to.eql(true); });
if (json && json.data) {
  pm.environment.set("access_token", json.data.accessToken || "");
  pm.environment.set("refresh_token", json.data.refreshToken || "");
  pm.environment.set("user_id", json.data.userId || "");
}`;

const collection = {
  info: {
    name: 'SoulMatch API Collection',
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
    description: 'Complete SoulMatch API collection for functional testing, smoke testing, and controlled load-test preparation. Do not run write-heavy requests at high concurrency in production.'
  },
  auth: bearer(),
  variable: [
    { key: 'base_url', value: 'http://20.204.142.19/api/v1' },
    { key: 'public_origin', value: 'http://20.204.142.19' }
  ],
  item: [
    folder('00 Public and Health', [
      request({ name: 'Runtime Config', method: 'GET', path: '/public/config', auth: noAuth }),
      request({
        name: 'Track Analytics Event',
        method: 'POST',
        path: '/public/analytics',
        auth: noAuth,
        body: rawJson({
          eventType: 'api_collection_smoke',
          serviceName: 'postman',
          page: 'collection',
          payload: { source: 'api-testing' }
        })
      }),
      request({ name: 'Public Profile', method: 'GET', path: '/public/profiles/{{target_profile_id}}', auth: noAuth }),
      request({ name: 'Landing Page', method: 'GET', path: '/public/landing-pages/home', auth: noAuth }),
      request({ name: 'Share Profile Page', method: 'GET', path: '{{public_origin}}/share/profile/{{target_profile_id}}', auth: noAuth }),
      request({ name: 'Share Landing Page', method: 'GET', path: '{{public_origin}}/share/landing/home', auth: noAuth })
    ]),
    folder('01 Auth', [
      request({ name: 'Send OTP', method: 'POST', path: '/auth/send-otp', auth: noAuth, body: rawJson({ phone: '{{phone}}' }) }),
      request({ name: 'Verify OTP', method: 'POST', path: '/auth/verify-otp', auth: noAuth, body: rawJson({ phone: '{{phone}}', otp: '{{otp}}' }), tests: setAuthTests }),
      request({ name: 'Google Login', method: 'POST', path: '/auth/google-login', auth: noAuth, body: rawJson({ googleToken: '{{google_id_token}}' }), tests: setAuthTests }),
      request({ name: 'Firebase Phone Login', method: 'POST', path: '/auth/firebase-phone-login', auth: noAuth, body: rawJson({ firebaseToken: '{{firebase_id_token}}', phone: '{{phone}}' }), tests: setAuthTests }),
      request({
        name: 'Refresh Token',
        method: 'POST',
        path: '/auth/refresh-token',
        auth: noAuth,
        body: rawJson({ refreshToken: '{{refresh_token}}' }),
        tests: `const json = pm.response.json();
if (json && json.data) {
  pm.environment.set("access_token", json.data.accessToken || pm.environment.get("access_token"));
  pm.environment.set("refresh_token", json.data.refreshToken || pm.environment.get("refresh_token"));
}`
      }),
      request({ name: 'Logout', method: 'POST', path: '/auth/logout', body: rawJson({ refreshToken: '{{refresh_token}}' }) })
    ]),
    folder('02 Profile', [
      request({
        name: 'Create/Update Profile Step',
        method: 'POST',
        path: '/profile/create',
        body: rawJson({
          step: 1,
          firstName: 'API',
          lastName: 'Tester',
          gender: 'male',
          dob: '1995-01-01',
          religion: 'Hindu',
          caste: 'Any',
          motherTongue: 'Telugu',
          maritalStatus: 'never_married'
        })
      }),
      request({
        name: 'My Profile',
        method: 'GET',
        path: '/profile/me',
        tests: `const json = pm.response.json();
if (json && json.data && json.data.profile_id) {
  pm.environment.set("profile_id", json.data.profile_id);
}`
      }),
      request({ name: 'Get Profile', method: 'GET', path: '/profile/{{target_profile_id}}' }),
      request({ name: 'Update Profile', method: 'PUT', path: '/profile/{{profile_id}}', body: rawJson({ profileCreatedBy: 'self' }) }),
      request({ name: 'Profile Completion', method: 'GET', path: '/profile/{{profile_id}}/completion' }),
      request({ name: 'Update Profile Status', method: 'PATCH', path: '/profile/status', body: rawJson({ profileStatus: 'active' }) }),
      request({ name: 'Get Preferences', method: 'GET', path: '/profile/{{profile_id}}/preferences' }),
      request({
        name: 'Update Preferences v2',
        method: 'PUT',
        path: '/profile/{{profile_id}}/preferences',
        body: rawJson({
          ageMin: 24,
          ageMax: 32,
          religion: 'Hindu',
          manglikPref: 'any',
          educationLevels: ['Graduate', 'Post Graduate'],
          occupations: ['IT', 'Doctor'],
          annualIncomeMin: 5,
          annualIncomeMax: 30,
          heightMinCm: 150,
          heightMaxCm: 180,
          locations: ['Hyderabad', 'Bangalore'],
          locationRadiusKm: 100,
          dietPrefs: ['vegetarian'],
          maritalStatuses: ['never_married'],
          familyTypes: ['nuclear'],
          relocationOpen: true,
          timeline: '6-12 months',
          dealBreakers: ['smoking'],
          goodToHave: ['same city']
        })
      }),
      request({ name: 'Update Privacy', method: 'PUT', path: '/profile/{{profile_id}}/privacy', body: rawJson({ photoPrivacy: 'all', profileVisibility: 'all' }) }),
      request({ name: 'Record View', method: 'POST', path: '/profile/{{target_profile_id}}/view', body: rawJson({ source: 'postman' }) }),
      request({ name: 'Recent Viewers', method: 'GET', path: '/profile/{{profile_id}}/viewers' }),
      request({ name: 'Submit Verification', method: 'POST', path: '/profile/{{profile_id}}/verifications', body: rawJson({ type: 'profile', documentUrl: 'https://example.com/proof.pdf' }) }),
      request({ name: 'Get Verifications', method: 'GET', path: '/profile/{{profile_id}}/verifications' }),
      request({ name: 'Get Photos', method: 'GET', path: '/profile/{{profile_id}}/photos' }),
      request({
        name: 'Upload Photos',
        method: 'POST',
        path: '/profile/{{profile_id}}/photos',
        body: { mode: 'formdata', formdata: [{ key: 'photos', type: 'file', src: [] }] }
      }),
      request({ name: 'Set Primary Photo', method: 'PUT', path: '/profile/{{profile_id}}/photos/{{photo_id}}/primary' }),
      request({ name: 'Delete Photo', method: 'DELETE', path: '/profile/{{profile_id}}/photos/{{photo_id}}' }),
      request({ name: 'Request Photo Access', method: 'POST', path: '/profile/{{target_profile_id}}/photo-access/request', body: rawJson({ message: 'Requesting photo access for family review.' }) }),
      request({ name: 'Photo Access Requests', method: 'GET', path: '/profile/photo-access/requests' }),
      request({ name: 'Respond Photo Access', method: 'PUT', path: '/profile/photo-access/requests/{{photo_access_request_id}}', body: rawJson({ status: 'approved' }) }),
      request({ name: 'Family Decisions', method: 'GET', path: '/profile/family-decisions' }),
      request({ name: 'Upsert Family Decision', method: 'PUT', path: '/profile/family-decisions/{{target_profile_id}}', body: rawJson({ status: 'family_review', familyVote: 'discuss', note: 'Need family input', nextStep: 'Call family' }) }),
      request({ name: 'Add Family Comment', method: 'POST', path: '/profile/family-decisions/{{family_decision_id}}/comments', body: rawJson({ vote: 'approve', comment: 'Looks suitable.' }) }),
      request({ name: 'Assist Status', method: 'GET', path: '/profile/assist/status' }),
      request({ name: 'Update Assist Status', method: 'PUT', path: '/profile/assist/status', body: rawJson({ isOptedIn: true, supportLevel: 'advisor_assisted', preferredContactWindow: 'Evening', familyContactName: 'Parent', familyContactPhone: '{{phone}}', notes: 'Need local advisor support.' }) }),
      request({ name: 'Record Match Feedback', method: 'POST', path: '/profile/{{target_profile_id}}/match-feedback', body: rawJson({ action: 'more_like_this', reason: 'good_location_fit', note: 'Similar family background preferred', metadata: { source: 'postman' } }) }),
      request({ name: 'Block Profile', method: 'POST', path: '/profile/{{target_profile_id}}/block', body: rawJson({ reason: 'not_interested' }) }),
      request({ name: 'Report Profile', method: 'POST', path: '/profile/{{target_profile_id}}/report', body: rawJson({ reason: 'fake_profile', description: 'Testing report flow.' }) })
    ]),
    folder('03 Matches and Search', [
      request({
        name: 'Recommended Matches',
        method: 'GET',
        path: '/matches/recommended?page=1&limit=25&verifiedOnly=false',
        tests: `const json = pm.response.json();
const first = json && json.data && json.data.matches && json.data.matches[0];
if (first && first.profileId) pm.environment.set("target_profile_id", first.profileId);
if (first && first.userId) pm.environment.set("target_user_id", first.userId);`
      }),
      request({ name: 'Recommended Matches Verified Only', method: 'GET', path: '/matches/recommended?page=1&limit=25&verifiedOnly=true' }),
      request({ name: 'Compatibility', method: 'GET', path: '/matches/compatibility/{{target_profile_id}}' }),
      request({ name: 'Basic Search', method: 'POST', path: '/search/basic', body: rawJson({ ageMin: 24, ageMax: 34, city: 'Hyderabad', verifiedOnly: false, page: 1, limit: 20 }) }),
      request({ name: 'Advanced Search', method: 'POST', path: '/search/advanced', body: rawJson({ ageMin: 24, ageMax: 34, religion: 'Hindu', education: 'Graduate', occupation: 'IT', diet: 'vegetarian', verifiedOnly: true, page: 1, limit: 20 }) }),
      request({ name: 'Saved Searches', method: 'GET', path: '/search/saved' }),
      request({ name: 'Save Search', method: 'POST', path: '/search/save', body: rawJson({ label: 'Hyderabad verified matches', ageMin: 24, ageMax: 34, city: 'Hyderabad', verifiedOnly: true }) })
    ]),
    folder('04 Interests', [
      request({ name: 'Send Interest', method: 'POST', path: '/interests/send', body: rawJson({ receiverId: '{{target_profile_id}}' }) }),
      request({ name: 'Received Interests', method: 'GET', path: '/interests/received' }),
      request({ name: 'Sent Interests', method: 'GET', path: '/interests/sent' }),
      request({ name: 'Respond Interest', method: 'PUT', path: '/interests/{{interest_id}}/respond', body: rawJson({ status: 'accepted' }) }),
      request({ name: 'Toggle Shortlist', method: 'POST', path: '/interests/shortlist/{{target_profile_id}}' }),
      request({ name: 'Get Shortlist', method: 'GET', path: '/interests/shortlist' })
    ]),
    folder('05 Chat', [
      request({ name: 'Conversations', method: 'GET', path: '/chat/conversations' }),
      request({ name: 'Messages', method: 'GET', path: '/chat/{{chat_id}}/messages?page=1&limit=50' }),
      request({ name: 'Chat Eligibility', method: 'GET', path: '/chat/eligibility/{{target_user_id}}' }),
      request({ name: 'Report Message', method: 'POST', path: '/chat/messages/{{message_id}}/report', body: rawJson({ reason: 'abusive', description: 'Testing message report.' }) })
    ]),
    folder('06 Notifications', [
      request({ name: 'Notifications', method: 'GET', path: '/notifications' }),
      request({ name: 'Register FCM Token', method: 'POST', path: '/notifications/devices/fcm-token', body: rawJson({ token: '{{fcm_token}}' }) }),
      request({ name: 'Mark Notification Read', method: 'PUT', path: '/notifications/{{notification_id}}/read' }),
      request({ name: 'Mark All Read', method: 'PUT', path: '/notifications/mark-all-read' }),
      request({ name: 'Internal Send Push', method: 'POST', path: '/notifications/send', auth: noAuth, headers: [{ key: 'x-internal-service-secret', value: '{{internal_service_secret}}' }], body: rawJson({ userId: '{{user_id}}', title: 'API test', body: 'Internal notification test', data: { source: 'postman' } }) }),
      request({ name: 'Internal Send Template', method: 'POST', path: '/notifications/template', auth: noAuth, headers: [{ key: 'x-internal-service-secret', value: '{{internal_service_secret}}' }], body: rawJson({ userId: '{{user_id}}', templateKey: 'interest_sent', variables: { name: 'API Tester' } }) })
    ]),
    folder('07 Payments', [
      request({ name: 'Plans', method: 'GET', path: '/payment/plans', auth: noAuth }),
      request({ name: 'Upgrade Packages', method: 'GET', path: '/payment/upgrade-packages', auth: noAuth }),
      request({
        name: 'Create Order',
        method: 'POST',
        path: '/payment/create-order',
        body: rawJson({ planId: '{{plan_id}}' }),
        tests: `const json = pm.response.json();
if (json && json.data) {
  pm.environment.set("payment_order_id", json.data.paymentOrderId || json.data.orderId || "");
  pm.environment.set("razorpay_order_id", json.data.razorpayOrderId || json.data.providerOrderId || json.data.id || "");
}`
      }),
      request({ name: 'Verify Payment', method: 'POST', path: '/payment/verify', body: rawJson({ orderId: '{{razorpay_order_id}}', paymentId: '{{razorpay_payment_id}}', signature: '{{razorpay_signature}}', planId: '{{plan_id}}' }) }),
      request({ name: 'Current Subscription', method: 'GET', path: '/payment/subscription' }),
      request({ name: 'Invoices / Subscription History', method: 'GET', path: '/payment/invoices' }),
      request({
        name: 'Razorpay Webhook',
        method: 'POST',
        path: '/payment/webhook',
        auth: noAuth,
        headers: [{ key: 'x-razorpay-signature', value: '{{razorpay_webhook_signature}}' }],
        body: rawJson({ event: 'payment.captured', payload: { payment: { entity: { id: '{{razorpay_payment_id}}', order_id: '{{razorpay_order_id}}', status: 'captured' } } } }),
        description: 'Requires valid Razorpay webhook signature. This request is for endpoint visibility, not normal manual execution.'
      })
    ]),
    folder('08 Admin', [
      request({
        name: 'Admin Login',
        method: 'POST',
        path: '/admin/login',
        auth: noAuth,
        body: rawJson({ email: '{{admin_email}}', password: '{{admin_password}}' }),
        tests: `const json = pm.response.json();
if (json && json.data) pm.environment.set("admin_token", json.data.token || json.data.accessToken || "");`
      }),
      ...[
        ['Dashboard', 'GET', '/admin/dashboard'],
        ['Realtime Snapshot', 'GET', '/admin/realtime/snapshot'],
        ['Users', 'GET', '/admin/users'],
        ['Profiles', 'GET', '/admin/profiles'],
        ['Advisors', 'GET', '/admin/advisors'],
        ['Assisted Assignments', 'GET', '/admin/assisted-assignments'],
        ['Pending Verifications', 'GET', '/admin/verifications'],
        ['Reports', 'GET', '/admin/reports'],
        ['Moderation Reports', 'GET', '/admin/moderation/reports'],
        ['Moderation Chat Logs', 'GET', '/admin/moderation/chat-logs'],
        ['Payments', 'GET', '/admin/payments'],
        ['Alerts', 'GET', '/admin/alerts'],
        ['Audit Logs', 'GET', '/admin/audit-logs'],
        ['Roles', 'GET', '/admin/roles'],
        ['Stories', 'GET', '/admin/stories'],
        ['Config', 'GET', '/admin/config'],
        ['Landing Pages', 'GET', '/admin/landing-pages'],
        ['Referrals', 'GET', '/admin/referrals'],
        ['Analytics Funnel', 'GET', '/admin/analytics/funnel'],
        ['Analytics Events', 'GET', '/admin/analytics/events'],
        ['Service Health', 'GET', '/admin/service-health']
      ].map(([name, method, url]) => request({ name, method, path: url, auth: bearer('{{admin_token}}') })),
      request({ name: 'Ban User', method: 'PUT', path: '/admin/users/{{target_user_id}}/ban', auth: bearer('{{admin_token}}'), body: rawJson({ reason: 'test' }) }),
      request({ name: 'Unban User', method: 'PUT', path: '/admin/users/{{target_user_id}}/unban', auth: bearer('{{admin_token}}') }),
      request({ name: 'Create Profile', method: 'POST', path: '/admin/profiles', auth: bearer('{{admin_token}}'), body: rawJson({ firstName: 'Admin', lastName: 'Created', gender: 'female', dob: '1998-01-01' }) }),
      request({ name: 'Bulk Create Profiles', method: 'POST', path: '/admin/profiles/bulk', auth: bearer('{{admin_token}}'), body: rawJson({ count: 5, gender: 'female' }) }),
      request({ name: 'Update Profile Admin', method: 'PUT', path: '/admin/profiles/{{target_profile_id}}', auth: bearer('{{admin_token}}'), body: rawJson({ verificationStatus: 'pending' }) }),
      request({ name: 'Delete Profile Admin', method: 'DELETE', path: '/admin/profiles/{{target_profile_id}}', auth: bearer('{{admin_token}}') }),
      request({ name: 'Update Profile Admin Status', method: 'PUT', path: '/admin/profiles/{{target_profile_id}}/status', auth: bearer('{{admin_token}}'), body: rawJson({ status: 'active' }) }),
      request({ name: 'Create Advisor', method: 'POST', path: '/admin/advisors', auth: bearer('{{admin_token}}'), body: rawJson({ fullName: 'Test Advisor', phone: '+919999999999', city: 'Hyderabad', state: 'Telangana', pincode: '500001' }) }),
      request({ name: 'Update Advisor', method: 'PUT', path: '/admin/advisors/{{advisor_id}}', auth: bearer('{{admin_token}}'), body: rawJson({ serviceLabel: 'Family Advisor' }) }),
      request({ name: 'Update Advisor Status', method: 'PUT', path: '/admin/advisors/{{advisor_id}}/status', auth: bearer('{{admin_token}}'), body: rawJson({ status: 'active', kycStatus: 'approved' }) }),
      request({ name: 'Update Assisted Assignment', method: 'PUT', path: '/admin/assisted-assignments/{{assignment_id}}', auth: bearer('{{admin_token}}'), body: rawJson({ requestStatus: 'assigned', followUpStatus: 'pending' }) }),
      request({ name: 'Approve Verification', method: 'PUT', path: '/admin/verifications/{{verification_id}}/approve', auth: bearer('{{admin_token}}'), body: rawJson({ note: 'Approved by API test' }) }),
      request({ name: 'Reject Verification', method: 'PUT', path: '/admin/verifications/{{verification_id}}/reject', auth: bearer('{{admin_token}}'), body: rawJson({ reason: 'Document unclear' }) }),
      request({ name: 'Resolve Report', method: 'PUT', path: '/admin/reports/{{report_id}}/resolve', auth: bearer('{{admin_token}}'), body: rawJson({ status: 'resolved', note: 'Reviewed' }) }),
      request({ name: 'Create Refund', method: 'POST', path: '/admin/payments/refunds', auth: bearer('{{admin_token}}'), body: rawJson({ transactionId: '{{transaction_id}}', reason: 'test_refund' }) }),
      request({ name: 'Acknowledge Alert', method: 'PUT', path: '/admin/alerts/{{alert_id}}/ack', auth: bearer('{{admin_token}}') }),
      request({ name: 'Create Campaign', method: 'POST', path: '/admin/campaigns', auth: bearer('{{admin_token}}'), body: rawJson({ name: 'API Test Campaign', channel: 'push', message: 'Testing campaign creation' }) }),
      request({ name: 'Approve Story', method: 'PUT', path: '/admin/stories/{{story_id}}/approve', auth: bearer('{{admin_token}}') }),
      request({ name: 'Update Config', method: 'PUT', path: '/admin/config/{{config_key}}', auth: bearer('{{admin_token}}'), body: rawJson({ value: { enabled: true } }) }),
      request({ name: 'Upsert Landing Page', method: 'POST', path: '/admin/landing-pages', auth: bearer('{{admin_token}}'), body: rawJson({ slug: 'home', title: 'SoulMatch', body: 'Landing page' }) }),
      request({ name: 'Update Landing Page', method: 'PUT', path: '/admin/landing-pages/{{landing_slug}}', auth: bearer('{{admin_token}}'), body: rawJson({ title: 'Updated Landing Page' }) }),
      request({ name: 'Create Referral Code', method: 'POST', path: '/admin/referrals/codes', auth: bearer('{{admin_token}}'), body: rawJson({ code: 'API2026', maxUses: 100 }) })
    ])
  ]
};

const environment = {
  name: 'SoulMatch Production Test Environment',
  values: [
    ['base_url', 'http://20.204.142.19/api/v1'],
    ['public_origin', 'http://20.204.142.19'],
    ['phone', '+919999999999'],
    ['otp', '123456'],
    ['access_token', ''],
    ['refresh_token', ''],
    ['user_id', ''],
    ['profile_id', ''],
    ['target_profile_id', ''],
    ['target_user_id', ''],
    ['photo_id', ''],
    ['photo_access_request_id', ''],
    ['family_decision_id', ''],
    ['interest_id', ''],
    ['chat_id', ''],
    ['message_id', ''],
    ['notification_id', ''],
    ['fcm_token', ''],
    ['plan_id', 'classic_3m'],
    ['payment_order_id', ''],
    ['razorpay_order_id', ''],
    ['razorpay_payment_id', ''],
    ['razorpay_signature', ''],
    ['razorpay_webhook_signature', ''],
    ['admin_email', 'admin@soulmatch.local'],
    ['admin_password', ''],
    ['admin_token', ''],
    ['advisor_id', ''],
    ['assignment_id', ''],
    ['verification_id', ''],
    ['report_id', ''],
    ['transaction_id', ''],
    ['alert_id', ''],
    ['story_id', ''],
    ['config_key', 'monetization'],
    ['landing_slug', 'home'],
    ['internal_service_secret', ''],
    ['google_id_token', ''],
    ['firebase_id_token', '']
  ].map(([key, value]) => ({ key, value, type: 'default', enabled: true }))
};

const k6Script = `import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 5),
      duration: __ENV.DURATION || '1m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1200'],
  },
};

const BASE_URL = (__ENV.BASE_URL || 'http://20.204.142.19/api/v1').replace(/\\/$/, '');
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || '';

function headers() {
  return ACCESS_TOKEN
    ? { Authorization: \`Bearer \${ACCESS_TOKEN}\`, 'Content-Type': 'application/json' }
    : { 'Content-Type': 'application/json' };
}

export default function () {
  const publicConfig = http.get(\`\${BASE_URL}/public/config\`);
  check(publicConfig, { 'public config 200': (r) => r.status === 200 });

  const plans = http.get(\`\${BASE_URL}/payment/plans\`);
  check(plans, { 'plans 200': (r) => r.status === 200 });

  if (ACCESS_TOKEN) {
    const me = http.get(\`\${BASE_URL}/profile/me\`, { headers: headers() });
    check(me, { 'profile me 200': (r) => r.status === 200 });

    const matches = http.get(\`\${BASE_URL}/matches/recommended?page=1&limit=25&verifiedOnly=false\`, { headers: headers() });
    check(matches, { 'matches 200': (r) => r.status === 200 });

    const search = http.post(
      \`\${BASE_URL}/search/basic\`,
      JSON.stringify({ ageMin: 24, ageMax: 34, city: 'Hyderabad', page: 1, limit: 20 }),
      { headers: headers() }
    );
    check(search, { 'search 200': (r) => r.status === 200 });

    const notifications = http.get(\`\${BASE_URL}/notifications\`, { headers: headers() });
    check(notifications, { 'notifications ok': (r) => [200, 204].includes(r.status) });

    const subscription = http.get(\`\${BASE_URL}/payment/subscription\`, { headers: headers() });
    check(subscription, { 'subscription 200': (r) => r.status === 200 });
  }

  sleep(Number(__ENV.SLEEP || 1));
}
`;

const readme = `# SoulMatch API Testing and Load Testing

Generated from the current backend routes and Android API client.

## Files

| File | Purpose |
| --- | --- |
| \`soulmatch.postman_collection.json\` | Functional API collection for Postman/Newman |
| \`soulmatch.postman_environment.json\` | Environment variables with safe placeholders |
| \`k6/soulmatch-smoke-load.js\` | Read-heavy k6 smoke/load script |

## Import in Postman

1. Open Postman.
2. Click \`Import\`.
3. Import \`api-testing/soulmatch.postman_collection.json\`.
4. Import \`api-testing/soulmatch.postman_environment.json\`.
5. Select environment \`SoulMatch Production Test Environment\`.
6. Set \`phone\`, \`otp\`, \`access_token\`, or admin values as needed.

## Functional Test Flow

Recommended order:

1. \`00 Public and Health -> Runtime Config\`
2. \`01 Auth -> Verify OTP\` or paste a known \`access_token\`
3. \`02 Profile -> My Profile\`
4. \`03 Matches and Search -> Recommended Matches\`
5. \`03 Matches and Search -> Basic Search\`
6. \`04 Interests -> Send Interest\`
7. \`06 Notifications -> Notifications\`
8. \`07 Payments -> Plans\`
9. \`07 Payments -> Create Order\`

## Load Testing

Install k6:

Windows:

\`\`\`powershell
winget install k6.k6
\`\`\`

Run a safe public-only load test:

\`\`\`powershell
k6 run api-testing/k6/soulmatch-smoke-load.js
\`\`\`

Run authenticated load test:

\`\`\`powershell
$env:BASE_URL="http://20.204.142.19/api/v1"
$env:ACCESS_TOKEN="PASTE_USER_ACCESS_TOKEN"
$env:VUS="10"
$env:DURATION="2m"
k6 run api-testing/k6/soulmatch-smoke-load.js
\`\`\`

Important:

- Do not load test \`/auth/send-otp\` against Firebase or SMS providers. It can cost money and trigger abuse limits.
- Do not load test payment create/verify endpoints on production without a separate Razorpay test plan.
- Start with 5 to 10 virtual users on the current Azure VM. The VM is small and can be overloaded quickly.
- For production readiness testing, use a staging VM and seeded test accounts.

## Newman CLI

Install:

\`\`\`powershell
npm install -g newman
\`\`\`

Run collection:

\`\`\`powershell
newman run api-testing/soulmatch.postman_collection.json -e api-testing/soulmatch.postman_environment.json
\`\`\`

For write-heavy folders, run manually and carefully. The collection includes destructive/admin actions for completeness.
`;

fs.mkdirSync(k6Dir, { recursive: true });
fs.writeFileSync(path.join(outDir, 'soulmatch.postman_collection.json'), JSON.stringify(collection, null, 2));
fs.writeFileSync(path.join(outDir, 'soulmatch.postman_environment.json'), JSON.stringify(environment, null, 2));
fs.writeFileSync(path.join(k6Dir, 'soulmatch-smoke-load.js'), k6Script);
fs.writeFileSync(path.join(outDir, 'README.md'), readme);
console.log(`Generated API test assets in ${outDir}`);
