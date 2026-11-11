package in.abhiikhula.claim.controller;

import in.abhiikhula.claim.dto.ClaimResponseDto;
import in.abhiikhula.claim.dto.ReviewClaimRequest;
import in.abhiikhula.claim.model.ClaimStatus;
import in.abhiikhula.claim.service.ShopClaimService;
import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.common.exception.UnauthorizedException;
import in.abhiikhula.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/claims")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "M6 Shopkeeper Onboarding - Admin Review", description = "Admin Review & Approval of Shop Claims (BRD FR-604, FR-605)")
public class AdminClaimController {

    private final ShopClaimService shopClaimService;

    @GetMapping
    @Operation(summary = "Get claims by status for admin review", description = "Returns claims filtered by status (default: PENDING) for admin review (BRD FR-604)")
    public ResponseEntity<ApiResponse<List<ClaimResponseDto>>> getClaims(
            @Parameter(description = "Filter by status (PENDING, APPROVED, REJECTED)")
            @RequestParam(required = false, defaultValue = "PENDING") ClaimStatus status,
            @AuthenticationPrincipal User admin) {

        if (admin == null) {
            throw new UnauthorizedException("Admin authentication required");
        }

        List<ClaimResponseDto> claims = shopClaimService.getClaimsByStatus(status, admin);
        return ResponseEntity.ok(ApiResponse.ok(claims, "Claims queue fetched successfully"));
    }

    @PatchMapping("/{claimId}/review")
    @Operation(summary = "Approve or Reject shop claim", description = "Admin decision on claim. On approval, shop owner is linked and user role upgraded to SHOP_OWNER (BRD FR-604, FR-605)")
    public ResponseEntity<ApiResponse<ClaimResponseDto>> reviewClaim(
            @PathVariable UUID claimId,
            @Valid @RequestBody ReviewClaimRequest request,
            @AuthenticationPrincipal User admin) {

        if (admin == null) {
            throw new UnauthorizedException("Admin authentication required");
        }

        ClaimResponseDto reviewed = shopClaimService.reviewClaim(claimId, request, admin);
        String message = request.getStatus() == ClaimStatus.APPROVED ? "Shop claim approved successfully" : "Shop claim rejected";
        return ResponseEntity.ok(ApiResponse.ok(reviewed, message));
    }
}

