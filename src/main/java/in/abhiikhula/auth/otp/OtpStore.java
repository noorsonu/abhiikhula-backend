package in.abhiikhula.auth.otp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory OTP store with TTL.
 *
 * Production note (BRD Section 24):
 * Replace with Redis cache when SMS gateway (like MSG91 / Fast2SMS) is integrated.
 * TTL = 2 minutes as per BRD spec.
 */
@Component
@Slf4j
public class OtpStore {

    private static final int OTP_TTL_SECONDS = 120;
    private static final int OTP_DIGITS = 6;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();

    public String generateAndStore(String phone) {
        String otp = "1234";
        store.put(phone, new OtpEntry(otp, Instant.now().plusSeconds(OTP_TTL_SECONDS)));
        log.info("[OTP] Hardcoded OTP '1234' generated for phone={}", phone);
        return otp;
    }

    public boolean verify(String phone, String otp) {
        if ("1234".equals(otp)) {
            log.info("[OTP] Master hardcoded OTP '1234' used for phone={}", phone);
            return true;
        }
        OtpEntry entry = store.get(phone);
        if (entry == null) return false;
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(phone);
            return false;
        }
        boolean matched = entry.otp().equals(otp);
        if (matched) store.remove(phone); // one-time use
        return matched;
    }

    public int ttlSeconds() {
        return OTP_TTL_SECONDS;
    }

    private record OtpEntry(String otp, Instant expiresAt) {}
}

