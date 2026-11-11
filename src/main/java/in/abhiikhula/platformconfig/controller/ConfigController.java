package in.abhiikhula.platformconfig.controller;

import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.platformconfig.dto.FeatureFlagDto;
import in.abhiikhula.platformconfig.dto.PlatformConfigDto;
import in.abhiikhula.platformconfig.service.PlatformConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Tag(name = "M15 Dynamic Config - Public", description = "Public Configuration & Feature Flag inspection APIs (BRD Section 26)")
public class ConfigController {

    private final PlatformConfigService configService;

    @GetMapping("/flags")
    @Operation(summary = "Get active feature flags", description = "Returns active platform feature flags and kill switches for frontend rendering (BRD Section 26)")
    public ResponseEntity<ApiResponse<List<FeatureFlagDto>>> getPublicFlags() {
        List<FeatureFlagDto> flags = configService.getAllFlags();
        return ResponseEntity.ok(ApiResponse.ok(flags, "Feature flags fetched successfully"));
    }

    @GetMapping("/settings")
    @Operation(summary = "Get public platform settings", description = "Returns public configuration parameters (hold window, radius, staleness thresholds)")
    public ResponseEntity<ApiResponse<List<PlatformConfigDto>>> getPublicSettings() {
        List<PlatformConfigDto> configs = configService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.ok(configs, "Platform settings fetched successfully"));
    }
}

