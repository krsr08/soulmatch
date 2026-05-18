import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    matches_feed: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 50),
      duration: __ENV.DURATION || '5m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
  },
};

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:3003/api/v1').replace(/\/$/, '');
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || '';

export default function () {
  const response = http.get(`${BASE_URL}/matches/recommended?page=1&limit=25`, {
    headers: ACCESS_TOKEN ? { Authorization: `Bearer ${ACCESS_TOKEN}` } : {},
  });
  check(response, {
    'matches accepted': (r) => [200, 401, 403].includes(r.status),
    'matches capped latency': (r) => r.timings.duration < 1000,
  });
  sleep(Number(__ENV.SLEEP || 1));
}
