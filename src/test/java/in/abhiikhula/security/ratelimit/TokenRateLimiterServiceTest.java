package in.abhiikhula.security.ratelimit;

import in.abhiikhula.platformconfig.service.PlatformConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenRateLimiterServiceTest {

    @Mock
    private PlatformConfigService platformConfigService;

    private TokenRateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        when(platformConfigService.getIntConfig(eq("rate_limit_tokens_per_min"), eq(5))).thenReturn(5);
        rateLimiterService = new TokenRateLimiterService(platformConfigService);
    }

    @Test
    @DisplayName("Allows up to 5 token requests per minute from same IP (BRD FR-1604)")
    void testAllowsUpToFiveRequestsPerMinute() {
        String clientIp = "192.168.1.100";

        for (int i = 1; i <= 5; i++) {
            boolean allowed = rateLimiterService.tryAcquire(clientIp);
            assertTrue(allowed, "Request " + i + " should be allowed");
        }

        // 6th request within same minute must be rejected
        boolean sixthAllowed = rateLimiterService.tryAcquire(clientIp);
        assertFalse(sixthAllowed, "6th request within 1 minute must be rejected with 429");
    }

    @Test
    @DisplayName("Different IPs have independent rate limit buckets")
    void testIndependentIpBuckets() {
        String ip1 = "10.0.0.1";
        String ip2 = "10.0.0.2";

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiterService.tryAcquire(ip1));
        }
        assertFalse(rateLimiterService.tryAcquire(ip1));

        // ip2 should still have full quota
        assertTrue(rateLimiterService.tryAcquire(ip2));
    }

    @Test
    @DisplayName("Resetting IP bucket immediately restores quota")
    void testResetRestoresQuota() {
        String clientIp = "172.16.0.5";

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiterService.tryAcquire(clientIp));
        }
        assertFalse(rateLimiterService.tryAcquire(clientIp));

        rateLimiterService.reset(clientIp);

        assertTrue(rateLimiterService.tryAcquire(clientIp));
    }
}

