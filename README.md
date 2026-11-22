# NexusGate — Distributed Reactive API Gateway & Rate-Limiting Mesh

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.2-green.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud Gateway](https://img.shields.io/badge/Spring_Cloud-Gateway_Reactive-blue.svg)](https://spring.io/projects/spring-cloud-gateway)
[![Redis](https://img.shields.io/badge/Redis-Sliding_Window_Limiter-red.svg)](https://redis.io/)
[![Resilience4j](https://img.shields.io/badge/Resilience4j-Circuit_Breakers-yellow.svg)](https://resilience4j.readme.io/)
[![k6 Benchmark](https://img.shields.io/badge/k6_Load_Test-24.8k_RPS-purple.svg)](benchmarks/BENCHMARK_REPORT.md)
[![Prometheus](https://img.shields.io/badge/Prometheus-Metrics_Exporter-e6522c.svg)](https://prometheus.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

An enterprise-grade, high-throughput reactive reverse proxy and API security mesh built with **Java 21**, **Spring Cloud Gateway (Project Reactor / WebFlux)**, **Redis**, and **Resilience4j**. 

Engineered for microservice ecosystems requiring non-blocking edge routing, token bucket sliding-window throttling, chaos-tested circuit-breaker fallbacks, and microsecond-level telemetry. Benchmarked at **24,850+ req/sec** with **p99 latency < 8.76ms** under heavy load.

---

## 🏛️ System Architecture

```
                                  [External Client Traffic]
                                             │
                                             ▼
                               ┌───────────────────────────┐
                               │   NexusGate Edge Gateway  │ (Netty Non-Blocking Reactor)
                               │         Port: 8080        │
                               └─────────────┬─────────────┘
                                             │
             ┌───────────────────────────────┼───────────────────────────────┐
             ▼                               ▼                               ▼
   ┌───────────────────┐           ┌───────────────────┐           ┌───────────────────┐
   │  Dynamic Routing  │           │   Sliding-Window  │           │   Resilience4j    │
   │  & JWT Validation │           │   Rate Limiter    │           │  Circuit Breaker  │
   └─────────┬─────────┘           └─────────┬─────────┘           └─────────┬─────────┘
             │                               │                               │
             │                     ┌─────────┴─────────┐                     │
             │                     ▼                   ▼                     ▼
             │             ┌───────────────┐   ┌───────────────┐   ┌───────────────────┐
             │             │ Redis Cluster │   │ Memory Ring   │   │ Fallback Response │
             │             │ Sorted Sets   │   │ Buffer (Skip- │   │ Queue (HTTP 503)  │
             │             │ (Atomic ZSet) │   │ ListSet)      │   └───────────────────┘
             │             └───────────────┘   └───────────────┘
             │
             ├───────────────────────────────────────────────────────┐
             ▼                                                       ▼
   ┌───────────────────┐                                   ┌───────────────────┐
   │   order-service   │ (Port: 8081)                      │   user-service    │ (Port: 8082)
   │  Inventory & Saga │                                   │  RBAC & Profiles  │
   └───────────────────┘                                   └───────────────────┘
```

---

## ⚡ Core Engineering & Architectural Capabilities

### 1. Non-Blocking Reactive Netty Engine
* Built entirely on Spring WebFlux and Project Reactor event loops ($2 \times \text{vCPU}$ worker threads). 
* Eliminates thread-per-request blocking, sustaining 24,000+ concurrent client connections with minimal heap allocation (< 200MB). Read the full rationale in [ADR 001: Netty vs Tomcat](docs/ADR.md#adr-001-reactive-netty-event-loops-vs-thread-per-request-tomcat).

### 2. Dual-Tier Atomic Sliding-Window Rate Limiter
* **Tier 1 (Distributed)**: Uses Redis Sorted Sets (`ZADD`, `ZREMRANGEBYSCORE`, `ZCARD`) to evaluate client request frequencies inside a dynamic moving window (10s). Eliminates boundary-burst evasion where naive fixed-window counters allow $2 \times N$ traffic spikes across minute boundaries.
* **Tier 2 (Resilient Fallback)**: In-memory `ConcurrentSkipListSet` ring buffer activates automatically during Redis connectivity blips, preventing fail-open security bypasses.
* **RFC 6585 Compliance**: Returns `HTTP 429 Too Many Requests` with `Retry-After: 10`, `X-RateLimit-Limit`, and `X-RateLimit-Remaining: 0`.

### 3. Fault Tolerance & Chaos Engineering (Resilience4j)
* Monitors downstream microservice health over a 10-call sliding window.
* Automatically trips into `OPEN` state upon detecting a 50% failure rate, fast-failing subsequent requests in **< 0.8ms** instead of waiting for downstream socket timeouts.
* Live chaos endpoints (`/api/v1/chaos/inject-fault`) allow simulating microservice outages and verifying state machine transitions (`CLOSED` &rarr; `OPEN` &rarr; `HALF-OPEN`).

### 4. Telemetry & Observability
* Exposes live Prometheus metrics via Spring Boot Actuator (`/actuator/prometheus`) tracking JVM garbage collection, Netty thread pool allocation, p95/p99 routing latency, and rate-limit drops.

---

## 📊 Performance Benchmarks (k6 Load Test)

NexusGate was benchmarked using distributed virtual users via `benchmarks/load-test.js`. Full details and methodology available in [BENCHMARK_REPORT.md](benchmarks/BENCHMARK_REPORT.md).

| Metric | Result | Target SLA | Status |
| :--- | :--- | :--- | :--- |
| **Peak Throughput** | **24,850 req/sec** | > 15,000 req/sec | ✅ PASS |
| **Median Latency (p50)** | **1.62 ms** | < 5.0 ms | ✅ PASS |
| **95th Percentile (p95)** | **5.12 ms** | < 15.0 ms | ✅ PASS |
| **99th Percentile (p99)** | **8.76 ms** | < 25.0 ms | ✅ PASS |
| **Rate Limiter Precision** | **100.0%** (0 false permits) | 99.9% | ✅ PASS |

To reproduce the benchmark:
```bash
docker compose up -d
k6 run benchmarks/load-test.js
```

---

## 🚀 Quickstart & Local Setup

### 1. Launch the Complete Multi-Service Mesh (Docker Compose)
Boot the Gateway, upstream `order-service`, `user-service`, Redis, Prometheus, and Grafana in a single command:
```bash
git clone https://github.com/VaibhavBajpaij/nexusgate-api-gateway.git
cd nexusgate-api-gateway
docker compose up -d
```

### 2. Verify Mesh Endpoints
```bash
# 1. Gateway Health
curl http://localhost:8080/actuator/health

# 2. Test Sliding-Window Rate Limiting (Limit: 5 req / 10s)
for i in {1..7}; do curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/v1/ratelimit/test; done
# Output: 200, 200, 200, 200, 200, 429, 429

# 3. Inspect Resilience4j Circuit Breaker State
curl http://localhost:8080/api/v1/circuitbreaker/status

# 4. Inject Chaos Fault
curl http://localhost:8080/api/v1/chaos/inject-fault?status=500
```

### 3. Build & Test Locally (Maven)
```bash
mvn clean test
mvn spring-boot:run
```

---

## 📚 Architectural Documentation
- [Architecture Decision Records (ADRs)](docs/ADR.md)
- [Performance Benchmark Report](benchmarks/BENCHMARK_REPORT.md)
- [k6 Load Testing Script](benchmarks/load-test.js)

---

## 👤 Author
**Vaibhav Bajpai**
* GitHub: [@VaibhavBajpaij](https://github.com/VaibhavBajpaij)
* LinkedIn: [in/vaibhav-bajpai18](https://www.linkedin.com/in/vaibhav-bajpai18/)
* Email: [vaibhav.bajpai.tech@gmail.com](mailto:vaibhav.bajpai.tech@gmail.com)
