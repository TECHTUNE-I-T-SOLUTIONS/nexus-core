package com.techtune.nexus.webhook.rate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter.
 * 
 * Rate limits are scoped to tenant IDs. This is not cluster-wide - it only works
 * within a single JVM instance. For cluster-wide rate limiting, use Redis or a
 * distributed cache.
 */
@Component
public class RateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);
    
    private final ConcurrentMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> windowStarts = new ConcurrentHashMap<>();
    
    private final int maxRequests;
    private final long windowMillis;
    
    public RateLimiter(
            @org.springframework.beans.factory.annotation.Value("${nexus.webhook.rate-limit.max-requests:100}") int maxRequests,
            @org.springframework.beans.factory.annotation.Value("${nexus.webhook.rate-limit.window-seconds:60}") int windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
        logger.info("Rate limiter initialized: {} requests per {} seconds", maxRequests, windowSeconds);
    }
    
    /**
     * Checks if a request from the given tenant is allowed.
     * 
     * @param tenantId The tenant ID to rate limit
     * @return true if the request is allowed, false if rate limited
     */
    public boolean tryAcquire(String tenantId) {
        if (tenantId == null) {
            // No rate limiting for unauthenticated requests (should not happen in practice)
            return true;
        }
        
        String key = tenantId.toString();
        long now = System.currentTimeMillis();
        
        // Get or create the request counter
        AtomicInteger count = requestCounts.computeIfAbsent(key, k -> new AtomicInteger(0));
        Long windowStart = windowStarts.computeIfAbsent(key, k -> now);
        
        // Check if we need to reset the window
        if (now - windowStart > windowMillis) {
            // New window - reset counters
            count.set(0);
            windowStarts.put(key, now);
        }
        
        // Check if we're under the limit
        if (count.incrementAndGet() <= maxRequests) {
            return true;
        }
        
        // Rate limited - decrement back and log
        count.decrementAndGet();
        logger.warn("Rate limit exceeded for tenant {}: {} requests in {} ms window", 
                tenantId, count.get(), windowMillis);
        return false;
    }
    
    /**
     * Resets the rate limit for a specific tenant (useful for testing).
     */
    public void reset(String tenantId) {
        if (tenantId != null) {
            requestCounts.remove(tenantId.toString());
            windowStarts.remove(tenantId.toString());
        }
    }
}
