package in.abhiikhula.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "System Uptime & Health Probe (BRD Section 30)")
public class HealthController {

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
}

