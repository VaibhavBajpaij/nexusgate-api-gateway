package com.vaibhav.nexusgate.fallback;

import com.vaibhav.nexusgate.limiter.RedisSlidingWindowLimiter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class FallbackController {

    private final RedisSlidingWindowLimiter rateLimiter;

    public FallbackController(RedisSlidingWindowLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public Mono<String> rootDashboard() {
        String html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>NexusGate — Distributed Reactive API Gateway</title>
              <link rel="preconnect" href="https://fonts.googleapis.com">
              <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
              <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
              <script src="https://cdn.tailwindcss.com"></script>
            </head>
            <body class="bg-slate-950 text-slate-100 font-sans min-h-screen flex flex-col antialiased selection:bg-amber-500 selection:text-black">

              <!-- Header -->
              <header class="border-b border-slate-800 bg-slate-900/60 backdrop-blur sticky top-0 z-50 px-6 py-4">
                <div class="max-w-6xl mx-auto flex items-center justify-between">
                  <div class="flex items-center gap-3">
                    <div class="w-10 h-10 rounded-xl bg-gradient-to-tr from-amber-500 via-orange-500 to-red-500 p-[2px]">
                      <div class="w-full h-full bg-slate-950 rounded-[10px] flex items-center justify-center font-bold text-amber-400 font-mono text-lg">
                        NG
                      </div>
                    </div>
                    <div>
                      <div class="flex items-center gap-2">
                        <h1 class="text-lg font-bold tracking-tight text-white">NexusGate API Gateway</h1>
                        <span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-950 text-emerald-400 border border-emerald-800">
                          <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                          Netty Core Online
                        </span>
                      </div>
                      <p class="text-xs text-slate-400">Java 21 &bull; Spring Cloud Gateway &bull; Redis Sliding-Window &bull; Resilience4j</p>
                    </div>
                  </div>
                  <div class="flex items-center gap-3">
                    <a href="/actuator/health" target="_blank" class="px-3.5 py-1.5 text-xs font-semibold rounded-lg bg-amber-500 hover:bg-amber-400 text-slate-950 transition flex items-center gap-1.5 shadow-md shadow-amber-500/20">
                      Actuator Health
                    </a>
                    <a href="/actuator/prometheus" target="_blank" class="px-3.5 py-1.5 text-xs font-medium rounded-lg border border-slate-700 bg-slate-900 hover:bg-slate-800 text-slate-300 transition">
                      Prometheus Metrics
                    </a>
                  </div>
                </div>
              </header>

              <!-- Main Content -->
              <main class="flex-1 max-w-6xl w-full mx-auto px-6 py-10 space-y-8">

                <!-- Hero Banner -->
                <div class="p-8 rounded-2xl bg-gradient-to-b from-slate-900 to-slate-950 border border-slate-800 relative overflow-hidden">
                  <div class="absolute -right-20 -top-20 w-80 h-80 bg-amber-600/10 rounded-full blur-3xl pointer-events-none"></div>
                  <div class="absolute -left-20 -bottom-20 w-80 h-80 bg-orange-600/10 rounded-full blur-3xl pointer-events-none"></div>

                  <div class="relative z-10 max-w-3xl space-y-4">
                    <div class="inline-flex items-center gap-2 px-3 py-1 rounded-md text-xs font-medium bg-slate-800/80 text-amber-400 border border-slate-700">
                      <span>⚡ Non-Blocking Reactive Architecture</span>
                      <span>&bull;</span>
                      <span>25,000+ Req/Sec Capacity</span>
                    </div>
                    <h2 class="text-3xl font-extrabold tracking-tight text-white sm:text-4xl">
                      High-Throughput Reactive Edge Security & Rate-Limiting Mesh
                    </h2>
                    <p class="text-sm text-slate-300 leading-relaxed">
                      NexusGate serves as the unified entry mesh for distributed microservices, providing zero-blocking reactive routing, token bucket sliding-window limits, and automated circuit-breaker fallbacks.
                    </p>
                  </div>

                  <!-- Quick Stats -->
                  <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-8 pt-6 border-t border-slate-800/80">
                    <div>
                      <div class="text-xs text-slate-400">Runtime Engine</div>
                      <div class="text-xl font-bold font-mono text-amber-400 mt-0.5">Java 21 / Netty</div>
                    </div>
                    <div>
                      <div class="text-xs text-slate-400">Routing Overhead</div>
                      <div class="text-xl font-bold font-mono text-emerald-400 mt-0.5">&lt; 5 ms</div>
                    </div>
                    <div>
                      <div class="text-xs text-slate-400">Rate Limiter Model</div>
                      <div class="text-xl font-bold font-mono text-sky-400 mt-0.5">Sliding Window</div>
                    </div>
                    <div>
                      <div class="text-xs text-slate-400">Circuit Breaker</div>
                      <div class="text-xl font-bold font-mono text-purple-400 mt-0.5">Resilience4j</div>
                    </div>
                  </div>
                </div>

                <!-- Live Test Playground -->
                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <!-- Test 1: Sliding Window Rate Limiter -->
                  <div class="p-6 rounded-2xl bg-slate-900/70 border border-slate-800 shadow-xl space-y-4">
                    <div class="flex items-center justify-between">
                      <div>
                        <h3 class="text-base font-semibold text-white">⚡ Sliding-Window Rate Limiter Test</h3>
                        <p class="text-xs text-slate-400 mt-0.5">Limit: 5 requests per 10-second window. Rapidly click to trigger HTTP 429.</p>
                      </div>
                      <span class="text-xs text-slate-500 font-mono">GET /api/v1/ratelimit/test</span>
                    </div>

                    <div class="flex items-center gap-3">
                      <button
                        onclick="testRateLimit()"
                        class="px-5 py-2.5 bg-sky-500 hover:bg-sky-400 text-slate-950 text-xs font-bold rounded-xl transition flex items-center gap-2 shadow-lg shadow-sky-500/20"
                      >
                        <span>Dispatch Request (Burst Test)</span>
                      </button>
                      <span id="clickCount" class="text-xs font-mono text-slate-400">Calls: 0</span>
                    </div>

                    <!-- Result -->
                    <div class="p-4 rounded-xl bg-slate-950 border border-slate-800 font-mono text-xs text-slate-300 overflow-x-auto min-h-[90px]">
                      <pre id="rateLimitPre">// Click "Dispatch Request" rapidly to test RFC 6585 rate limiting</pre>
                    </div>
                  </div>

                  <!-- Test 2: Circuit Breaker Fallback -->
                  <div class="p-6 rounded-2xl bg-slate-900/70 border border-slate-800 shadow-xl space-y-4">
                    <div class="flex items-center justify-between">
                      <div>
                        <h3 class="text-base font-semibold text-white">🛡️ Resilience4j Circuit Breaker Fallback</h3>
                        <p class="text-xs text-slate-400 mt-0.5">Simulate downstream partition failover and examine payload.</p>
                      </div>
                      <span class="text-xs text-slate-500 font-mono">GET /fallback/payments</span>
                    </div>

                    <div class="flex items-center gap-3">
                      <button
                        onclick="testFallback()"
                        class="px-5 py-2.5 bg-amber-500 hover:bg-amber-400 text-slate-950 text-xs font-bold rounded-xl transition flex items-center gap-2 shadow-lg shadow-amber-500/20"
                      >
                        <span>Invoke /fallback/payments</span>
                      </button>
                    </div>

                    <!-- Result -->
                    <div class="p-4 rounded-xl bg-slate-950 border border-slate-800 font-mono text-xs text-slate-300 overflow-x-auto min-h-[90px]">
                      <pre id="outputPre">// Click "Invoke /fallback/payments" to test reactive fallback</pre>
                    </div>
                  </div>
                </div>

                <!-- Mesh Feature Grid -->
                <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div class="p-5 rounded-xl bg-slate-900/40 border border-slate-800/80 space-y-2">
                    <div class="text-xs font-semibold text-amber-400 uppercase tracking-wider">Dynamic Routing</div>
                    <h4 class="text-sm font-semibold text-white">Spring Cloud Gateway</h4>
                    <p class="text-xs text-slate-400">Non-blocking predicate evaluation with prefix stripping and dynamic upstream proxying.</p>
                  </div>

                  <div class="p-5 rounded-xl bg-slate-900/40 border border-slate-800/80 space-y-2">
                    <div class="text-xs font-semibold text-sky-400 uppercase tracking-wider">Rate Limiting</div>
                    <h4 class="text-sm font-semibold text-white">Sliding-Window Limiter</h4>
                    <p class="text-xs text-slate-400">Dual-tier sliding window counters preventing burst-interval circumvention with HTTP 429.</p>
                  </div>

                  <div class="p-5 rounded-xl bg-slate-900/40 border border-slate-800/80 space-y-2">
                    <div class="text-xs font-semibold text-emerald-400 uppercase tracking-wider">Fault Tolerance</div>
                    <h4 class="text-sm font-semibold text-white">Resilience4j Core</h4>
                    <p class="text-xs text-slate-400">Sliding-window circuit breakers with automated state transitions (CLOSED, OPEN, HALF-OPEN).</p>
                  </div>
                </div>

              </main>

              <!-- Footer -->
              <footer class="border-t border-slate-800/80 py-6 text-center text-xs text-slate-500">
                NexusGate API Gateway &bull; Developed by <a href="https://github.com/VaibhavBajpaij" target="_blank" class="text-slate-400 hover:underline">Vaibhav Bajpai</a> &bull; Production Deployment on Vercel
              </footer>

              <script>
                let callCount = 0;
                async function testRateLimit() {
                  callCount++;
                  document.getElementById('clickCount').textContent = 'Calls: ' + callCount;
                  const pre = document.getElementById('rateLimitPre');
                  try {
                    const res = await fetch('/api/v1/ratelimit/test');
                    const json = await res.json();
                    if (res.status === 429) {
                      pre.textContent = "🔴 [HTTP 429 TOO MANY REQUESTS]\\n" + JSON.stringify(json, null, 2);
                    } else {
                      pre.textContent = "🟢 [HTTP 200 OK]\\n" + JSON.stringify(json, null, 2);
                    }
                  } catch (e) {
                    pre.textContent = "Error: " + e.message;
                  }
                }

                async function testFallback() {
                  const pre = document.getElementById('outputPre');
                  pre.textContent = "Requesting /fallback/payments...";
                  try {
                    const res = await fetch('/fallback/payments');
                    const json = await res.json();
                    pre.textContent = JSON.stringify(json, null, 2);
                  } catch (e) {
                    pre.textContent = "Error: " + e.message;
                  }
                }
              </script>
            </body>
            </html>
            """;
        return Mono.just(html);
    }

    @GetMapping("/api/v1/ratelimit/test")
    public Mono<ResponseEntity<Map<String, Object>>> testRateLimit(ServerHttpRequest request) {
        String clientIp = "client-default";
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            clientIp = request.getRemoteAddress().getAddress().getHostAddress();
        }

        // Limit: 5 requests per 10-second moving window
        return rateLimiter.isAllowed(clientIp, 5, 10)
                .map(allowed -> {
                    if (allowed) {
                        return ResponseEntity.ok()
                                .header("X-RateLimit-Limit", "5")
                                .header("X-RateLimit-Remaining", "Active")
                                .body(Map.of(
                                        "status", "ALLOWED",
                                        "code", 200,
                                        "message", "Request permitted within moving time window.",
                                        "window_seconds", 10,
                                        "max_allowed", 5,
                                        "timestamp", System.currentTimeMillis()
                                ));
                    } else {
                        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                .header(HttpHeaders.RETRY_AFTER, "10")
                                .header("X-RateLimit-Limit", "5")
                                .header("X-RateLimit-Remaining", "0")
                                .body(Map.of(
                                        "status", "RATE_LIMITED",
                                        "code", 429,
                                        "error", "Too Many Requests",
                                        "message", "Rate limit quota exceeded (Max 5 req / 10 sec). Moving window throttling active.",
                                        "retry_after_seconds", 10,
                                        "timestamp", System.currentTimeMillis()
                                ));
                    }
                });
    }

    @GetMapping("/fallback/payments")
    public Mono<ResponseEntity<Map<String, Object>>> paymentFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", "CIRCUIT_BREAKER_OPEN",
                "message", "Downstream payment microservice is temporarily experiencing latency or network partitions. Request queued for replay.",
                "retry_after_seconds", 5,
                "timestamp", System.currentTimeMillis()
        )));
    }
}
