package in.abhiikhula.security.ratelimit;

import in.abhiikhula.platformconfig.service.PlatformConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * M16 Rate Limiter: Sliding-window in-memory IP rate limiter for queue token issuance.
 * Strictly implements BRD FR-1604: Rate limit token issue at 5 per minute per IP.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenRateLimiterService {

    private final PlatformConfigService platformConfigService;
    private final Map<String, Deque<Long>> ipRequestTimestamps = new ConcurrentHashMap<>();

    private static final long WINDOW_MS = 60_000L; // 1 minute sliding window
    private static final int DEFAULT_MAX_REQUESTS_PER_MINUTE = 5; // BRD FR-1604 default

    /**
     * Checks and records a token issuance request for the given client IP.
     *
     * @param clientIp Client IP address
     * @return true if request is permitted under rate limit, false if exceeded (429)
     */
    public synchronized boolean tryAcquire(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return true;
        }

        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_MS;

        int limit = platformConfigService != null
                ? platformConfigService.getIntConfig("rate_limit_tokens_per_min", DEFAULT_MAX_REQUESTS_PER_MINUTE)
                : DEFAULT_MAX_REQUESTS_PER_MINUTE;

        Deque<Long> timestamps = ipRequestTimestamps.computeIfAbsent(clientIp, k -> new ArrayDeque<>());

        // Evict timestamps outside the 1-minute sliding window
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= limit) {
            log.warn("Rate limit exceeded for IP {}: {} requests in 60s (limit: {})", clientIp, timestamps.size(), limit);
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    /**
     * Reset rate limit bucket for an IP (useful for testing and admin operations).
     */
    public synchronized void reset(String clientIp) {
        if (clientIp != null) {
            ipRequestTimestamps.remove(clientIp);
        }
    }

    /**
     * Clear all rate limits (useful for test suites).
     */
    public synchronized void resetAll() {
        ipRequestTimestamps.clear();
    }

    /**
     * Periodic background task to sweep idle IPs and prevent memory leaks.
     */
    @Scheduled(fixedRate = 300_000)
    public synchronized void cleanupOldEntries() {
        long windowStart = System.currentTimeMillis() - WINDOW_MS;
        ipRequestTimestamps.entrySet().removeIf(entry -> {
            Deque<Long> timestamps = entry.getValue();
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            return timestamps.isEmpty();
        });
    }
}

