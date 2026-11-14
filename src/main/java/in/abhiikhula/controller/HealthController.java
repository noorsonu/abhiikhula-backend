package in.abhiikhula.controller;

import in.abhiikhula.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Health & Maintenance", description = "System Uptime, Health Probe & Database Purge")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    @Operation(summary = "Health probe", description = "Check if AbhiiKhula backend service is running healthy")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString(),
                "service", "AbhiiKhula Backend",
                "version", "1.0.0"
        ));
    }

    @PostMapping("/system/purge-database")
    @Operation(summary = "Purge Database completely", description = "Truncates all transactional, queue, token, shop and user data for fresh production restart")
    public ResponseEntity<ApiResponse<Map<String, Object>>> purgeDatabase(
            @RequestParam(required = false, defaultValue = "confirm-purge-2026") String key) {
        log.warn("TRIGGERING FULL DATABASE PURGE ON ACTIVE DATASOURCE");

        jdbcTemplate.execute(
            "TRUNCATE TABLE analytics_events, disputes, featured_slots, invoices, notifications, " +
            "queue_sessions, reviews, shop_claims, shop_hours, shop_status_events, subscriptions, " +
            "tokens, shops, users CASCADE"
        );

        log.info("DATABASE PURGE SUCCESSFUL");
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of(
                    "status", "CLEAN_SLATE_SUCCESS",
                    "timestamp", Instant.now().toString(),
                    "message", "All shops, tokens, users, reviews, queues and events have been completely deleted from database."
                ),
                "Render database completely purged successfully"
        ));
    }
}
