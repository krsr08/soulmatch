import http from 'k6/http';
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

const BASE_URL = (__ENV.BASE_URL || 'http://20.204.142.19/api/v1').replace(/\/$/, '');
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || '';

function headers() {
  return ACCESS_TOKEN
    ? { Authorization: `Bearer ${ACCESS_TOKEN}`, 'Content-Type': 'application/json' }
    : { 'Content-Type': 'application/json' };
}

export default function () {
  const publicConfig = http.get(`${BASE_URL}/public/config`);
  check(publicConfig, { 'public config 200': (r) => r.status === 200 });

  const plans = http.get(`${BASE_URL}/payment/plans`);
  check(plans, { 'plans 200': (r) => r.status === 200 });

  if (ACCESS_TOKEN) {
    const me = http.get(`${BASE_URL}/profile/me`, { headers: headers() });
    check(me, { 'profile me 200': (r) => r.status === 200 });

    const matches = http.get(`${BASE_URL}/matches/recommended?page=1&limit=25&verifiedOnly=false`, { headers: headers() });
    check(matches, { 'matches 200': (r) => r.status === 200 });

    const search = http.post(
      `${BASE_URL}/search/basic`,
      JSON.stringify({ ageMin: 24, ageMax: 34, city: 'Hyderabad', page: 1, limit: 20 }),
      { headers: headers() }
    );
    check(search, { 'search 200': (r) => r.status === 200 });

    const notifications = http.get(`${BASE_URL}/notifications`, { headers: headers() });
    check(notifications, { 'notifications ok': (r) => [200, 204].includes(r.status) });

    const subscription = http.get(`${BASE_URL}/payment/subscription`, { headers: headers() });
    check(subscription, { 'subscription 200': (r) => r.status === 200 });
  }

  sleep(Number(__ENV.SLEEP || 1));
}
