package in.abhiikhula.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.auth.jwt.JwtAuthFilter;
import in.abhiikhula.auth.jwt.JwtUtil;
import in.abhiikhula.common.dto.ErrorResponse;
import in.abhiikhula.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtUtil jwtUtil;
        private final UserRepository userRepository;
        private final ObjectMapper objectMapper;
        private final in.abhiikhula.security.ratelimit.TokenRateLimiterService tokenRateLimiterService;

        @Bean
        public in.abhiikhula.security.ratelimit.TokenRateLimitFilter tokenRateLimitFilter() {
                return new in.abhiikhula.security.ratelimit.TokenRateLimitFilter(tokenRateLimiterService, objectMapper);
        }

        @Bean
        public JwtAuthFilter jwtAuthFilter() {
                return new JwtAuthFilter(jwtUtil, userRepository);
        }

        @Bean
        public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
                org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
                configuration.setAllowedOriginPatterns(java.util.List.of("*"));
                configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(java.util.List.of("*"));
                configuration.setAllowCredentials(true);
                org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                                        ErrorResponse err = ErrorResponse.builder()
                                                                        .status(HttpServletResponse.SC_UNAUTHORIZED)
                                                                        .errorCode("UNAUTHORIZED")
                                                                        .message("Full authentication is required to access this resource. Please provide a valid Bearer token.")
                                                                        .path(request.getRequestURI())
                                                                        .build();
                                                        response.getWriter()
                                                                        .write(objectMapper.writeValueAsString(err));
                                                })
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                                        ErrorResponse err = ErrorResponse.builder()
                                                                        .status(HttpServletResponse.SC_FORBIDDEN)
                                                                        .errorCode("FORBIDDEN")
                                                                        .message("You do not have permission to access this resource.")
                                                                        .path(request.getRequestURI())
                                                                        .build();
                                                        response.getWriter()
                                                                        .write(objectMapper.writeValueAsString(err));
                                                }))
                                .authorizeHttpRequests(auth -> auth
                                                // Protected merchant routes under /api/shops
                                                .requestMatchers(HttpMethod.GET, "/api/shops/my").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/shops/*/analytics/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/shops/*/subscription")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/shops/*/invoices")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/shops/*/featured-slots")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/shops/*/disputes")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/shops/*/disputes/*/reply")
                                                .authenticated()

                                                // Public discovery, onboarding, and health routes
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/system/purge-database").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/zones/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/ads/featured/availability")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/ads/featured/*/click")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/shops/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/shops").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/shops/*/queue/tokens/join").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/shops/*/tokens").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/config/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/tokens/*").permitAll()

                                                // Public dispute & support routes
                                                .requestMatchers(HttpMethod.POST, "/api/disputes").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/support/faq").permitAll()

                                                // Public review routes
                                                .requestMatchers(HttpMethod.POST, "/api/reviews").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/reviews/*/report-abuse")
                                                .permitAll()

                                                // Public analytics event ingestion
                                                .requestMatchers(HttpMethod.POST, "/api/analytics/events").permitAll()

                                                // Public auth login/OTP routes
                                                .requestMatchers(HttpMethod.POST, "/api/auth/send-otp").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/auth/verify-otp").permitAll()

                                                // Public API documentation
                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**")
                                                .permitAll()

                                                // Protected actions (Token Compulsory)
                                                .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/shops/**").authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/api/shops/**").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/zones/**").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/categories/**").authenticated()
                                                .requestMatchers("/api/queue/**").authenticated()
                                                .requestMatchers("/api/tokens/**").authenticated()
                                                .requestMatchers("/api/notifications/**").authenticated()
                                                .requestMatchers("/api/users/**").authenticated()
                                                .requestMatchers("/api/claims/**").authenticated()
                                                .requestMatchers("/api/admin/**").authenticated()

                                                .anyRequest().authenticated())
                                .addFilterBefore(tokenRateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
