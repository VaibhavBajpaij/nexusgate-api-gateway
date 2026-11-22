package com.vaibhav.nexusgate.limiter;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Component
public class RedisSlidingWindowLimiter {
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, ConcurrentSkipListSet<Long>> localRingBuffers = new ConcurrentHashMap<>();

    public RedisSlidingWindowLimiter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Dual-Tier Atomic Sliding-Window Counter Rate Limiter.
     * Tier 1: Redis Sorted Set (ZREMRANGEBYSCORE + ZCARD + ZADD) across distributed gateway clusters.
     * Tier 2: Resilient Concurrent Atomic Ring Buffer (ConcurrentSkipListSet) when Redis is absent.
     */
    public Mono<Boolean> isAllowed(String apiKey, int maxRequestsPerWindow, long windowSeconds) {
        String key = "ratelimit:" + apiKey;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSeconds * 1000);

        try {
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
                    .onErrorResume(e -> fallbackLocalSlidingWindow(apiKey, maxRequestsPerWindow, now, windowStart));
        } catch (Exception ex) {
            return fallbackLocalSlidingWindow(apiKey, maxRequestsPerWindow, now, windowStart);
        }
    }

    private Mono<Boolean> fallbackLocalSlidingWindow(String apiKey, int maxRequests, long now, long windowStart) {
        ConcurrentSkipListSet<Long> timestamps = localRingBuffers.computeIfAbsent(apiKey, k -> new ConcurrentSkipListSet<>());
        // Purge expired elements outside the moving time window
        timestamps.headSet(windowStart).clear();

        if (timestamps.size() >= maxRequests) {
            return Mono.just(false); // HTTP 429 Too Many Requests
        }
        timestamps.add(now);
        return Mono.just(true);
    }
}
