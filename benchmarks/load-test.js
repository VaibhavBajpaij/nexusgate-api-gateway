import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

export const errorRate = new Rate('errors');
export const rateLimitedRate = new Rate('rate_limited');

export const options = {
  stages: [
    { duration: '20s', target: 50 },    // Warm-up ramp
    { duration: '1m', target: 250 },    // Sustained high concurrency
    { duration: '30s', target: 1000 },  // Peak burst stress
    { duration: '20s', target: 0 },     // Graceful ramp-down
  ],
  thresholds: {
    'http_req_duration': ['p(95)<20', 'p(99)<45'], // 95% of requests under 20ms
    'errors': ['rate<0.01'],                       // < 1% unexpected errors
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  // Test 1: Actuator Telemetry Health Check
  const healthRes = http.get(`${BASE_URL}/actuator/health`);
  check(healthRes, {
    'gateway is up': (r) => r.status === 200 && r.json().status === 'UP',
  });

  // Test 2: Distributed Sliding-Window Rate Limiter
  // Distribute load across 25 simulated client IP pools
  const clientSubnet = `10.0.0.${__VU % 25}`;
  const rateLimitRes = http.get(`${BASE_URL}/api/v1/ratelimit/test`, {
    headers: { 'X-Forwarded-For': clientSubnet },
  });

  if (rateLimitRes.status === 429) {
    rateLimitedRate.add(1);
    check(rateLimitRes, {
      'rate limit triggered with 429': (r) => r.status === 429,
      'retry-after header present': (r) => r.headers['Retry-After'] !== undefined,
      'remaining quota header is zero': (r) => r.headers['X-Ratelimit-Remaining'] === '0',
    });
  } else if (rateLimitRes.status === 200) {
    rateLimitedRate.add(0);
    check(rateLimitRes, {
      'allowed request returned 200': (r) => r.status === 200,
      'status is ALLOWED': (r) => r.json().status === 'ALLOWED',
    });
  } else {
    errorRate.add(1);
  }

  // Small delay to simulate real client micro-pacing
  sleep(0.02);
}
