package com.techtune.nexus.webhook.rate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    private RateLimiter rateLimiter;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter(5, 60); // 5 requests per 60 seconds
        tenantId = UUID.randomUUID();
    }

    @Test
    void testAllowsRequestsUnderLimit() {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire(tenantId.toString()));
        }
    }

    @Test
    void testRejectsRequestsOverLimit() {
        // Exhaust the limit
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire(tenantId.toString()));
        }
        
        // Next request should be rate limited
        assertFalse(rateLimiter.tryAcquire(tenantId.toString()));
    }

    @Test
    void testSeparateTenantsHaveSeparateLimits() {
        UUID tenant2 = UUID.randomUUID();
        
        // Exhaust limit for tenant1
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire(tenantId.toString()));
        }
        assertFalse(rateLimiter.tryAcquire(tenantId.toString()));
        
        // Tenant2 should still be allowed
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire(tenant2.toString()));
        }
    }

    @Test
    void testAllowsNullTenantId() {
        // Null tenant ID should always be allowed (for unauthenticated paths)
        for (int i = 0; i < 100; i++) {
            assertTrue(rateLimiter.tryAcquire(null));
        }
    }

    @Test
    void testResetClearsCounters() {
        // Exhaust limit
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire(tenantId.toString()));
        }
        assertFalse(rateLimiter.tryAcquire(tenantId.toString()));
        
        // Reset
        rateLimiter.reset(tenantId.toString());
        
        // Should be allowed again
        assertTrue(rateLimiter.tryAcquire(tenantId.toString()));
    }
}
