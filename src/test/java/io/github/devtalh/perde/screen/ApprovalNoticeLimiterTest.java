package io.github.devtalh.perde.screen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApprovalNoticeLimiterTest {
    @Test void limitsEachDomainAcrossRequestersAndAllowsExactlyAtBoundary() {
        var limiter = new ApprovalNoticeLimiter();
        assertTrue(limiter.tryAcquire("example.com", 1000));
        assertFalse(limiter.tryAcquire("example.com", 1001));
        assertFalse(limiter.tryAcquire("example.com", 60999));
        assertTrue(limiter.tryAcquire("example.org", 2000));
        assertTrue(limiter.tryAcquire("example.com", 61000));
        assertFalse(limiter.tryAcquire("example.com", 61001));
        assertFalse(limiter.tryAcquire("example.org", 61000));
        assertTrue(limiter.tryAcquire("example.org", 62000));
    }
}
