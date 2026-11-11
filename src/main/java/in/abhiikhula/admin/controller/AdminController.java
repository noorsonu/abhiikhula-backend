package in.abhiikhula.admin.controller;

import in.abhiikhula.admin.dto.AdminShopModerationDto;
import in.abhiikhula.admin.dto.AdminUserModerationDto;
import in.abhiikhula.admin.dto.ForceShopStatusRequest;
import in.abhiikhula.admin.dto.PlatformMetricsDto;
import in.abhiikhula.admin.service.AdminGovernanceService;
import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.common.exception.UnauthorizedException;
import in.abhiikhula.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "M8 Admin Panel & Governance", description = "Platform Monitoring, Metrics & Content Governance (BRD Section 19, FR-801 to FR-808)")
public class AdminController {

    private final AdminGovernanceService adminGovernanceService;

    @GetMapping("/metrics")
    @Operation(summary = "Get platform-wide health metrics", description = "Returns system metrics: total users, active queues, shops, tokens today (BRD FR-801, FR-802)")
    public ResponseEntity<ApiResponse<PlatformMetricsDto>> getPlatformMetrics(
            @AuthenticationPrincipal User admin) {

        if (admin == null) {
            throw new UnauthorizedException("Admin authentication required");
        }

        PlatformMetricsDto metrics = adminGovernanceService.getPlatformMetrics(admin);
        return ResponseEntity.ok(ApiResponse.ok(metrics, "Platform metrics fetched successfully"));
    }

    @PatchMapping("/shops/{shopId}/toggle-active")
    @Operation(summary = "Suspend or reactivate shop listing", description = "Admin moderation action to toggle shop active state (BRD FR-804)")
    public ResponseEntity<ApiResponse<AdminShopModerationDto>> toggleShopActive(
            @PathVariable UUID shopId,
            @AuthenticationPrincipal User admin) {

        if (admin == null) {
            throw new UnauthorizedException("Admin authentication required");
        }

        AdminShopModerationDto dto = adminGovernanceService.toggleShopActive(shopId, admin);
        return ResponseEntity.ok(ApiResponse.ok(dto, dto.getMessage()));
    }

    @PatchMapping("/shops/{shopId}/force-status")
    @Operation(summary = "Force change shop operational status", description = "Emergency admin override to change shop status and record audit event (BRD FR-804)")
    public ResponseEntity<ApiResponse<AdminShopModerationDto>> forceShopStatus(
            @PathVariable UUID shopId,
            @Valid @RequestBody ForceShopStatusRequest request,
            @AuthenticationPrincipal User admin) {

        if (admin == null) {
            throw new UnauthorizedException("Admin authentication required");
        }

        AdminShopModerationDto dto = adminGovernanceService.forceShopStatus(shopId, request, admin);
        return ResponseEntity.ok(ApiResponse.ok(dto, dto.getMessage()));
    }

    @PatchMapping("/users/{userId}/block")
    @Operation(summary = "Block abusive user account", description = "Suspends user from logging in or issuing queue tokens and cancels active tokens (BRD FR-806)")
    public ResponseEntity<ApiResponse<AdminUserModerationDto>> blockUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal User admin) {

        if (admin == null) {
            throw new UnauthorizedException("Admin authentication required");
        }

        AdminUserModerationDto dto = adminGovernanceService.blockUser(userId, admin);
        return ResponseEntity.ok(ApiResponse.ok(dto, dto.getMessage()));
    }

    @PatchMapping("/users/{userId}/unblock")
    @Operation(summary = "Unblock user account", description = "Restores access for previously suspended user (BRD FR-806)")
    public ResponseEntity<ApiResponse<AdminUserModerationDto>> unblockUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal User admin) {

        if (admin == null) {
            throw new UnauthorizedException("Admin authentication required");
        }

        AdminUserModerationDto dto = adminGovernanceService.unblockUser(userId, admin);
        return ResponseEntity.ok(ApiResponse.ok(dto, dto.getMessage()));
    }

    @PatchMapping("/zones/{zoneId}/toggle-active")
    @Operation(summary = "Enable or disable pilot zone", description = "Toggles active status of a zone for discovery (BRD FR-803)")
    public ResponseEntity<ApiResponse<Boolean>> toggleZoneActive(
            @PathVariable UUID zoneId,
            @AuthenticationPrincipal User admin) {

        if (admin == null) {
            throw new UnauthorizedException("Admin authentication required");
        }

        boolean active = adminGovernanceService.toggleZoneActive(zoneId, admin);
        return ResponseEntity.ok(ApiResponse.ok(active, "Zone active state changed to: " + active));
    }

    @PatchMapping("/categories/{categoryId}/toggle-active")
    @Operation(summary = "Enable or disable category", description = "Toggles active status of a category for discovery (BRD FR-803)")
    public ResponseEntity<ApiResponse<Boolean>> toggleCategoryActive(
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal User admin) {

        if (admin == null) {
            throw new UnauthorizedException("Admin authentication required");
        }

        boolean active = adminGovernanceService.toggleCategoryActive(categoryId, admin);
        return ResponseEntity.ok(ApiResponse.ok(active, "Category active state changed to: " + active));
    }
}

