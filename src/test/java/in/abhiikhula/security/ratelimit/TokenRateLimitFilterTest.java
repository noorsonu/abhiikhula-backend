package in.abhiikhula.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenRateLimitFilterTest {

    @Mock
    private TokenRateLimiterService rateLimiterService;

    @Mock
    private FilterChain filterChain;

    private TokenRateLimitFilter filter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        filter = new TokenRateLimitFilter(rateLimiterService, objectMapper);
    }

    @Test
    @DisplayName("Filter passes allowed token request down filter chain")
    void testAllowedRequestProceeds() throws Exception {
        UUID shopId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/shops/" + shopId + "/queue/tokens/join");
        request.setRemoteAddr("203.0.113.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.tryAcquire("203.0.113.1")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Filter blocks excessive token requests with 429 and RFC-7807 ErrorResponse (BRD FR-1604)")
    void testExcessiveTokenRequestBlockedWith429() throws Exception {
        UUID shopId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/shops/" + shopId + "/queue/tokens/join");
        request.setRemoteAddr("203.0.113.2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.tryAcquire("203.0.113.2")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(429, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));
        assertTrue(response.getContentAsString().contains("RATE_LIMITED"));
        assertTrue(response.getContentAsString().contains("Hold on, try again in a minute."));
    }

    @Test
    @DisplayName("Non-token endpoints (e.g. GET /api/shops) bypass rate limiting")
    void testNonTokenEndpointsBypass() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/shops");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimiterService, never()).tryAcquire(anyString());
        verify(filterChain).doFilter(request, response);
    }
}

