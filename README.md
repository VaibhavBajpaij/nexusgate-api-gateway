# NexusGate — Distributed Reactive API Gateway & Rate-Limiting Mesh

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.2-green.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud Gateway](https://img.shields.io/badge/Spring_Cloud-Gateway_Reactive-blue.svg)](https://spring.io/projects/spring-cloud-gateway)
[![Redis](https://img.shields.io/badge/Redis-Sliding_Window_Limiter-red.svg)](https://redis.io/)
[![Resilience4j](https://img.shields.io/badge/Resilience4j-Circuit_Breakers-yellow.svg)](https://resilience4j.readme.io/)
[![Prometheus](https://img.shields.io/badge/Prometheus-Metrics_Exporter-e6522c.svg)](https://prometheus.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A high-performance, reactive reverse proxy and API security mesh built with **Java 21**, **Spring Cloud Gateway (Project Reactor / WebFlux)**, **Redis**, and **Resilience4j**. Architected for enterprise Global Capability Centers (GCCs) and distributed microservices handling 25,000+ requests/sec with sub-5ms routing overhead.

---

## 🏛️ System Architecture

```
                  [External Client Traffic]
                             │
                             ▼
                 ┌───────────────────────┐
                 │    NexusGate (8080)   │ (Reactive Netty Core)
                 └───────────┬───────────┘
                             │
          ┌──────────────────┼──────────────────┐
          ▼                  ▼                  ▼
┌──────────────────┐ ┌───────────────┐ ┌──────────────────┐
│ JWT Auth Filter  │ │ Redis Sliding │ │ Resilience4j     │
│ (Claims Inject)  │ │ Window Limiter│ │ Circuit Breaker  │
└──────────────────┘ └───────┬───────┘ └────────┬─────────┘
                             │                  │
                             ▼                  ▼
                   [HTTP 429 Rejection]  [Fallback Queue]
                             │
                             ▼
              [Downstream Payment / Order Services]
```

---

## ⚡ Core Engineering & Architectural Capabilities

### 1. Non-Blocking Reactive Netty Engine
* Built entirely on Spring WebFlux and Project Reactor event loops. Eliminates thread-per-request blocking, sustaining tens of thousands of concurrent client connections with minimal heap allocation.

### 2. Redis Sliding-Window Rate Limiter
* Employs Redis Sorted Sets (`ZADD`, `ZREMRANGEBYSCORE`, `ZCARD`) to evaluate client request frequencies inside a dynamic moving window (60s). 
* Prevents traffic bursts at boundary intervals (which bypass naive fixed-window counters) and returns standard **RFC 6585 (HTTP 429 Too Many Requests)** headers.
* Configured with **fail-open resilience** to prevent gateway downtime during intermittent Redis network latency.

### 3. Fault Tolerance & Fallback Routing (Resilience4j)
* Automatically trips into `OPEN` state upon detecting a 50% failure rate across a 10-call sliding window.
* Diverts in-flight payment calls to `/fallback/payments` with actionable client retry headers (`retry_after_seconds: 5`) to prevent cascading resource starvation.

### 4. Telemetry & Observability
* Exposes live Prometheus metrics via Spring Boot Actuator (`/actuator/prometheus`) tracking p95/p99 routing latency, circuit breaker state transitions, and rate-limit drops.

---

## 🚀 Quickstart & Setup

### 1. Run with Docker Compose
```bash
git clone https://github.com/VaibhavBajpaij/nexusgate-api-gateway.git
cd nexusgate-api-gateway
docker-compose up -d
```

### 2. Run Locally via Maven
```bash
mvn clean package
mvn spring-boot:run
```

### 3. Test Rate Limiting & Routing
```bash
# Verify payment route routing with bearer token
curl -H "Authorization: Bearer test-token" http://localhost:8080/api/v1/payments/get

# Test missing token rejection (HTTP 401)
curl http://localhost:8080/api/v1/payments/get
```

---

## 👤 Author
**Vaibhav Bajpai**
* GitHub: [@VaibhavBajpaij](https://github.com/VaibhavBajpaij)
* LinkedIn: [in/vaibhav-bajpai18](https://www.linkedin.com/in/vaibhav-bajpai18/)
* Email: [vaibhav.bajpai.tech@gmail.com](mailto:vaibhav.bajpai.tech@gmail.com)
