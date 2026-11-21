package com.vaibhav.nexusgate.fallback;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/payments")
    public Mono<ResponseEntity<Map<String, Object>>> paymentFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", "CIRCUIT_BREAKER_OPEN",
                "message", "Downstream payment microservice is temporarily experiencing latency or network partitions. Request queued for replay.",
                "retry_after_seconds", 5,
                "timestamp", System.currentTimeMillis()
        )));
    }
}
