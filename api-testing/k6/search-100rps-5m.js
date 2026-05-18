import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    search_100_rps: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 100),
      timeUnit: '1s',
      duration: __ENV.DURATION || '5m',
      preAllocatedVUs: Number(__ENV.PREALLOCATED_VUS || 60),
      maxVUs: Number(__ENV.MAX_VUS || 200),
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<300', 'p(99)<750'],
  },
};

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:3004/api/v1').replace(/\/$/, '');
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || '';

export default function () {
  const response = http.post(
    `${BASE_URL}/search/basic`,
    JSON.stringify({
      ageMin: 24,
      ageMax: 34,
      country: 'India',
      education: 'Graduate',
      page: 1,
      limit: 25,
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        ...(ACCESS_TOKEN ? { Authorization: `Bearer ${ACCESS_TOKEN}` } : {}),
      },
    }
  );
  check(response, {
    'search accepted': (r) => [200, 401, 403].includes(r.status),
    'search under 750ms': (r) => r.timings.duration < 750,
  });
}
