package in.abhiikhula.platformconfig.controller;

import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.common.exception.ForbiddenException;
import in.abhiikhula.common.exception.UnauthorizedException;
import in.abhiikhula.platformconfig.dto.FeatureFlagDto;
import in.abhiikhula.platformconfig.dto.PlatformConfigDto;
import in.abhiikhula.platformconfig.dto.ToggleFlagRequest;
import in.abhiikhula.platformconfig.dto.UpdateConfigRequest;
import in.abhiikhula.platformconfig.service.PlatformConfigService;
import in.abhiikhula.user.model.User;
import in.abhiikhula.user.model.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "M15 Dynamic Config - Admin", description = "Admin Dynamic Settings & 1-Tap Feature Flag Kill Switches (BRD Section 26, FR-807, FR-811)")
public class AdminConfigController {

    private final PlatformConfigService configService;

    @GetMapping("/configs")
    @Operation(summary = "Admin: List all platform configurations", description = "Returns all dynamic configuration key-values across global, zone, and shop scopes (BRD Section 26)")
    public ResponseEntity<ApiResponse<List<PlatformConfigDto>>> listConfigs(@AuthenticationPrincipal User admin) {
        validateAdmin(admin);
        List<PlatformConfigDto> configs = configService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.ok(configs, "Configurations fetched successfully"));
    }

    @PutMapping("/configs/{key}")
    @Operation(summary = "Admin: Update platform configuration", description = "Updates dynamic parameter (e.g. hold_window_minutes, stale_warn_minutes) without restart (BRD Section 26)")
    public ResponseEntity<ApiResponse<PlatformConfigDto>> updateConfig(
            @PathVariable String key,
            @Valid @RequestBody UpdateConfigRequest request,
            @AuthenticationPrincipal User admin) {
        validateAdmin(admin);
        PlatformConfigDto updated = configService.updateConfig(key, request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Configuration '" + key + "' updated successfully"));
    }

    @GetMapping("/flags")
    @Operation(summary = "Admin: List all feature flags", description = "Returns all feature flags and kill switches (BRD Section 26, FR-811)")
    public ResponseEntity<ApiResponse<List<FeatureFlagDto>>> listFlags(@AuthenticationPrincipal User admin) {
        validateAdmin(admin);
        List<FeatureFlagDto> flags = configService.getAllFlags();
        return ResponseEntity.ok(ApiResponse.ok(flags, "Feature flags fetched successfully"));
    }

    @PatchMapping("/flags/{key}")
    @Operation(summary = "Admin: Toggle feature flag / kill switch", description = "Instantly enables or disables a feature flag globally or per-zone (BRD Section 26, FR-811)")
    public ResponseEntity<ApiResponse<FeatureFlagDto>> toggleFlag(
            @PathVariable String key,
            @Valid @RequestBody ToggleFlagRequest request,
            @AuthenticationPrincipal User admin) {
        validateAdmin(admin);
        FeatureFlagDto toggled = configService.toggleFlag(key, request);
        return ResponseEntity.ok(ApiResponse.ok(toggled, "Feature flag '" + key + "' set to " + toggled.getEnabled()));
    }

    private void validateAdmin(User admin) {
        if (admin == null) {
            throw new UnauthorizedException("Admin authentication required");
        }
        if (admin.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Administrative privileges required for this action");
        }
    }
}

