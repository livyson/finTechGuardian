package com.fintechguardian.resilience.ratelimiting;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Rate Limiter avançado com diferentes estratégias para APIs FinTechGuardian
 * Implementa sliding window, token bucket e distributed rate limiting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdvancedRateLimiter {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final RedisTemplate<String, String> redisTemplate;
    
    // Cache local para evitar calls desnecessárias ao Redis
    private final Map<String, RateLimiterInfo> localCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastCheckTimes = new ConcurrentHashMap<>();

    /**
     * Rate limiting baseado em sliding window para APIs corporativas
     */
    public <T> T executeWithCorporateRateLimit(String apiName, String userId, Supplier<T> operation) {
        String key = String.format("corporate:%s:%s", userId, apiName);
        return executeWithSlidingWindow(key, operation, 500, Duration.ofMinutes(1));
    }

    /**
     * Rate limiting baseado em token bucket para APIs de alta frequência
     */
    public <T> T executeWithHighFrequencyLimit(String apiName, String clientId, Supplier<T> operation) {
        String key = String.format("hf:%s:%s", clientId, apiName);
        return executeWithTokenBucket(key, operation, 100, Duration.ofSeconds(10));
    }

    /**
     * Rate limiting baseado em leaky bucket para APIs críticas de compliance
     */
    public <T> T executeWithComplianceLimit(String apiName, Supplier<T> operation) {
        String key = String.format("compliance:%s", apiName);
        return executeWithLeakyBucket(key, operation, 50, Duration.ofMinutes(1));
    }

    /**
     * Rate limiting baseado em algoritmo sliding window
     */
    private <T> T executeWithSlidingWindow(String key, Supplier<T> operation, 
                                          int maxRequests, Duration windowSize) {
        
        if (!isAllowedSlidingWindow(key, maxRequests, windowSize)) {
            log.warn("Rate limit exceeded for sliding window key: {}", key);
            throw new RateLimitExceededException("Sliding window rate limit exceeded for key: " + key);
        }

        recordRequest(key);
        return operation.get();
    }

    /**
     * Rate limiting baseado em token bucket
     */
    private <T> T executeWithTokenBucket(String key, Supplier<T> operation, 
                                       int bucketSize, Duration refillPeriod) {
        
        if (!isAllowedTokenBucket(key, bucketSize, refillPeriod)) {
            log.warn("Rate limit exceeded for token bucket key: {}", key);
            throw new RateLimitExceededException("Token bucket rate limit exceeded for key: " + key);
        }

        consumeToken(key);
        return operation.get();
    }

    /**
     * Rate limiting baseado em leaky bucket
     */
    private <T> T executeWithLeakyBucket(String key, Supplier<T> operation, 
                                        int bucketCapacity, Duration leakRate) {
        
        if (!isAllowedLeakyBucket(key, bucketCapacity, leakRate)) {
            log.warn("Rate limit exceeded for leaky bucket key: {}", key);
            throw new RateLimitExceededException("Leaky bucket rate limit exceeded for key: " + key);
        }

        addDroplet(key);
        return operation.get();
    }

    /**
     * Implementação de sliding window usando Redis
     */
    private boolean isAllowedSlidingWindow(String key, int maxRequests, Duration windowSize) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowSize.toMillis();
        
        // Remove requisições antigas
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        
        // Conta requisições na janela atual
        Long currentRequests = redisTemplate.opsForZSet().count(key, windowStart, now);
        
        if (currentRequests != null && currentRequests >= maxRequests) {
            return false;
        }

        // Adiciona nova requisição
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        redisTemplate.expire(key, windowSize);
        
        return true;
    }

    /**
     * Implementação de token bucket distribuído
     */
    private boolean isAllowedTokenBucket(String key, int bucketSize, Duration refillPeriod) {
        String bucketKey = key + ":bucket";
        long now = System.currentTimeMillis();
        
        // Recupera estado do bucket
        String bucketInfo = redisTemplate.opsForValue().get(bucketKey);
        
        TokenBucket bucket;
        if (bucketInfo == null) {
            // Bucket novo
            bucket = new TokenBucket(bucketSize, now);
        } else {
            // Recupera bucket existente
            bucket = TokenBucket.fromString(bucketInfo);
            bucket.refill(bucketSize, refillPeriod, now);
        }

        if (bucket.tokens <= 0) {
            return false;
        }

        bucket.tokens--;
        redisTemplate.opsForValue().set(bucketKey, bucket.toString(), Duration.ofMinutes(15));
        
        return true;
    }

    /**
     * Implementação de leaky bucket distribuído
     */
    private boolean isAllowedLeakyBucket(String key, int bucketCapacity, Duration leakRate) {
        String bucketKey = key + ":leaky";
        long now = System.currentTimeMillis();
        
        String bucketInfo = redisTemplate.opsForValue().get(bucketKey);
        
        LeakyBucket bucket;
        if (bucketInfo == null) {
            bucket = new LeakyBucket(bucketCapacity, now);
        } else {
            bucket = LeakyBucket.fromString(bucketInfo);
            bucket.leak(leakRate, now);
        }

        if (bucket.currentLevel >= bucketCapacity) {
            return false;
        }

        bucket.currentLevel++;
        redisTemplate.opsForValue().set(bucketKey, bucket.toString(), Duration.ofMinutes(15));
        
        return true;
    }

    private void recordRequest(String key) {
        redisTemplate.opsForValue().increment(key + ":count");
        redisTemplate.expire(key + ":count", Duration.ofMinutes(1));
    }

    private void consumeToken(String key) {
        recordRequest(key);
    }

    private void addDroplet(String key) {
        recordRequest(key);
    }

    /**
     * Obtém estatísticas de rate limiting
     */
    public RateLimitStats getRateLimitStats(String key) {
        String countKey = key + ":count";
        Long requestCount = redisTemplate.opsForValue().get(countKey) != null ? 
                          Long.valueOf(redisTemplate.opsForValue().get(countKey)) : 0L;
        
        return RateLimitStats.builder()
            .key(key)
            .requestCount(requestCount)
            .lastRequestTime(getLastRequestTime(key))
            .currentAllowedRequestRate(calculateCurrentRate(key))
            .build();
    }

    private long getLastRequestTime(String key) {
        return lastCheckTimes.getOrDefault(key, 0L);
    }

    private double calculateCurrentRate(String key) {
        // Mock calculation
        return 0.0;
    }

    // Classes auxiliares
    private static class TokenBucket {
        private int tokens;
        private long lastRefillTime;

        public TokenBucket(int tokens, long lastRefillTime) {
            this.tokens = tokens;
            this.lastRefillTime = lastRefillTime;
        }

        public void refill(int bucketSize, Duration refillPeriod, long now) {
            long timePassed = now - lastRefillTime;
            long tokensToAdd = timePassed / refillPeriod.toMillis();
            
            tokens = Math.min(bucketSize, tokens + (int)tokensToAdd);
            lastRefillTime = now;
        }

        public static TokenBucket fromString(String data) {
            String[] parts = data.split(":");
            return new TokenBucket(Integer.parseInt(parts[0]), Long.parseLong(parts[1]));
        }

        @Override
        public String toString() {
            return tokens + ":" + lastRefillTime;
        }
    }

    private static class LeakyBucket {
        private double currentLevel;
        private long lastLeakTime;

        public LeakyBucket(int capacity, long lastLeakTime) {
            this.currentLevel = 0;
            this.lastLeakTime = lastLeakTime;
        }

        public void leak(Duration leakRate, long now) {
            long timePassed = now - lastLeakTime;
            double leakedAmount = timePassed * (1.0 / leakRate.toMillis());
            
            currentLevel = Math.max(0, currentLevel - leakedAmount);
            lastLeakTime = now;
        }

        public static LeakyBucket fromString(String data) {
            String[] parts = data.split(":");
            return new LeakyBucket(0, Long.parseLong(parts[1]));
        }

        @Override
        public String toString() {
            return currentLevel + ":" + lastLeakTime;
        }
    }

    public static class RateLimiterInfo {
        private final int remainingRequests;
        private final long resetTime;
        private final Duration windowSize;

        public RateLimiterInfo(int remainingRequests, long resetTime, Duration windowSize) {
            this.remainingRequests = remainingRequests;
            this.resetTime = resetTime;
            this.windowSize = windowSize;
        }

        public int getRemainingRequests() { return remainingRequests; }
        public long getResetTime() { return resetTime; }
        public Duration getWindowSize() { return windowSize; }
    }

    public static class RateLimitStats {
        private final String key;
        private final Long requestCount;
        private final long lastRequestTime;
        private final double currentAllowedRequestRate;

        @lombok.Builder
        public RateLimitStats(String key, Long requestCount, long lastRequestTime, double currentAllowedRequestRate) {
            this.key = key;
            this.requestCount = requestCount;
            this.lastRequestTime = lastRequestTime;
            this.currentAllowedRequestRate = currentAllowedRequestRate;
        }

        public String getKey() { return key; }
        public Long getRequestCount() { return requestCount; }
        public long getLastRequestTime() { return lastRequestTime; }
        public double getCurrentAllowedRequestRate() { return currentAllowedRequestRate; }
    }

    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}
