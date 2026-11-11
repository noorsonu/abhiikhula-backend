package in.abhiikhula.shop.controller;

import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.common.exception.UnauthorizedException;
import in.abhiikhula.shop.dto.ShopStatusResponseDto;
import in.abhiikhula.shop.dto.UpdateStatusRequest;
import in.abhiikhula.shop.service.ShopStatusService;
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
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@Tag(name = "M2 Live Status Engine", description = "Shop Status & Crowd Level Live Updates (BRD Section 13, 29)")
public class ShopStatusController {

    private final ShopStatusService shopStatusService;

    @PutMapping("/{shopId}/status")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update shop live status and crowd level", description = "Allows shop owner (or admin) to toggle OPEN/BUSY/PAUSED/CLOSED and crowd level 1-4. Writes immutable audit event.")
    public ResponseEntity<ApiResponse<ShopStatusResponseDto>> updateStatus(
            @PathVariable UUID shopId,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Full authentication is required to update shop status");
        }

        ShopStatusResponseDto response = shopStatusService.updateStatus(shopId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(response, "Shop status updated successfully"));
    }

    @GetMapping("/{shopId}/status")
    @Operation(summary = "Get current live shop status and recent timeline", description = "Public endpoint for customers to view real-time crowd level, wait times, and status transitions.")
    public ResponseEntity<ApiResponse<ShopStatusResponseDto>> getStatus(@PathVariable UUID shopId) {
        ShopStatusResponseDto response = shopStatusService.getStatus(shopId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Live status fetched successfully"));
    }
}

