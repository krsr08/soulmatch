import ws from 'k6/ws';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    chat_connections: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 200),
      duration: __ENV.DURATION || '5m',
    },
  },
  thresholds: {
    checks: ['rate>0.95'],
    ws_connecting: ['p(95)<1000'],
  },
};

const SOCKET_URL = (__ENV.SOCKET_URL || 'ws://localhost:3005/socket.io/?EIO=4&transport=websocket').replace(/\/$/, '');
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || '';

export default function () {
  const response = ws.connect(SOCKET_URL, {
    headers: ACCESS_TOKEN ? { Authorization: `Bearer ${ACCESS_TOKEN}` } : {},
    tags: { service: 'chat-service' },
  }, (socket) => {
    socket.on('open', () => {
      socket.setTimeout(() => socket.close(), Number(__ENV.HOLD_MS || 15000));
    });
    socket.on('message', () => {});
    socket.on('error', () => {});
  });
  check(response, {
    'chat socket accepted or auth denied safely': (r) => [101, 400, 401, 403].includes(r && r.status),
  });
  sleep(1);
}
