package com.vaibhav.nexusgate.limiter;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class RedisSlidingWindowLimiter {
    private final ReactiveStringRedisTemplate redisTemplate;

    public RedisSlidingWindowLimiter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Sliding Window Counter Rate Limiting Algorithm.
     * Evaluates requests inside a moving time window (default 60 seconds).
     */
    public Mono<Boolean> isAllowed(String apiKey, int maxRequestsPerWindow, long windowSeconds) {
        String key = "ratelimit:" + apiKey;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSeconds * 1000);

        // Redis Sorted Set sliding-window execution:
        // 1. ZREMRANGEBYSCORE key 0 windowStart (purge expired timestamps)
        // 2. ZCARD key (count current active requests)
        // 3. If count < maxRequests, ZADD key now now
        return redisTemplate.opsForZSet().removeRangeByScore(key, org.springframework.data.domain.Range.closed(0.0, (double) windowStart))
                .then(redisTemplate.opsForZSet().size(key))
                .flatMap(count -> {
                    if (count != null && count >= maxRequestsPerWindow) {
                        return Mono.just(false); // Rate limit exceeded (HTTP 429)
                    }
                    return redisTemplate.opsForZSet().add(key, String.valueOf(now), (double) now)
                            .then(redisTemplate.expire(key, java.time.Duration.ofSeconds(windowSeconds * 2)))
                            .thenReturn(true);
                })
                .onErrorReturn(true); // Fail-open on Redis timeout to preserve availability
    }
}
