package in.abhiikhula.claim.controller;

import in.abhiikhula.claim.dto.ClaimResponseDto;
import in.abhiikhula.claim.dto.SubmitClaimRequest;
import in.abhiikhula.claim.service.ShopClaimService;
import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.common.exception.UnauthorizedException;
import in.abhiikhula.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "M6 Shopkeeper Onboarding & Listing Claim", description = "Shop Claim Submission & Tracking (BRD Section 17, FR-601 to FR-607)")
public class ShopClaimController {

    private final ShopClaimService shopClaimService;

    @PostMapping("/shops/{shopId}/claim")
    @Operation(summary = "Submit claim for shop listing", description = "Submits a claim with verification proof (e.g. shop photo with banner or electricity bill) for admin review (BRD FR-601, FR-603)")
    public ResponseEntity<ApiResponse<ClaimResponseDto>> submitClaim(
            @PathVariable UUID shopId,
            @Valid @RequestBody SubmitClaimRequest request,
            @AuthenticationPrincipal User applicant) {

        if (applicant == null) {
            throw new UnauthorizedException("Authentication required to submit shop claim");
        }

        ClaimResponseDto claim = shopClaimService.submitClaim(shopId, request, applicant);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(claim, "Shop claim submitted successfully for admin review"));
    }

    @GetMapping("/claims/my")
    @Operation(summary = "Get my submitted claims", description = "Returns history of all claims submitted by authenticated user (BRD FR-601)")
    public ResponseEntity<ApiResponse<List<ClaimResponseDto>>> getMyClaims(
            @AuthenticationPrincipal User applicant) {

        if (applicant == null) {
            throw new UnauthorizedException("Authentication required");
        }

        List<ClaimResponseDto> claims = shopClaimService.getMyClaims(applicant);
        return ResponseEntity.ok(ApiResponse.ok(claims, "Submitted claims fetched successfully"));
    }
}
