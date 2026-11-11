package in.abhiikhula.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.common.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Filter that intercepts token issuance endpoints and enforces 5 tokens/min per IP rate limit (BRD FR-1604).
 */
@RequiredArgsConstructor
@Slf4j
public class TokenRateLimitFilter extends OncePerRequestFilter {

    private final TokenRateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    // Pattern matching token issue endpoints: /api/shops/{id}/queue/tokens/join, /api/shops/{id}/tokens, /api/shops/{id}/queue/walk-in
    private static final Pattern TOKEN_ISSUE_PATTERN = Pattern.compile(
            "^/api/shops/[^/]+/(queue/tokens/join|tokens|queue/walk-in)$"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equalsIgnoreCase(method) && TOKEN_ISSUE_PATTERN.matcher(path).matches()) {
            String clientIp = extractClientIp(request);

            if (!rateLimiterService.tryAcquire(clientIp)) {
                log.warn("Blocking token request from IP {} to path {} due to rate limit", clientIp, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                ErrorResponse error = ErrorResponse.builder()
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .errorCode("RATE_LIMITED")
                        .message("Hold on, try again in a minute.") // Exact BRD Annexure D copy
                        .path(path)
                        .timestamp(Instant.now())
                        .build();

                response.getWriter().write(objectMapper.writeValueAsString(error));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // First IP in comma-separated list is the client IP
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }
}

