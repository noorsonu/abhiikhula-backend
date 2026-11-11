package in.abhiikhula.dispute.controller;

import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.dispute.dto.DisputeResponseDto;
import in.abhiikhula.dispute.dto.MerchantReplyDisputeRequest;
import in.abhiikhula.dispute.service.DisputeService;
import in.abhiikhula.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/shops/{shopId}/disputes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "M13 Support & Disputes - Merchant", description = "Shopkeeper dispute viewing and response (BRD Section 24, FR-1304)")
public class MerchantDisputeController {

    private final DisputeService disputeService;

    @GetMapping
    @Operation(summary = "Get shop disputes", description = "Returns all dispute tickets filed against this shop with status event evidence (FR-1304)")
    public ResponseEntity<ApiResponse<List<DisputeResponseDto>>> getShopDisputes(
            @PathVariable UUID shopId,
            @AuthenticationPrincipal User owner) {
        List<DisputeResponseDto> disputes = disputeService.getShopDisputes(shopId, owner);
        return ResponseEntity.ok(ApiResponse.ok(disputes, "Shop disputes fetched successfully"));
    }

    @PostMapping("/{disputeId}/reply")
    @Operation(summary = "Merchant reply to dispute", description = "Allows shopkeeper to post their explanation/apology, moving state to IN_REVIEW (FR-1304)")
    public ResponseEntity<ApiResponse<DisputeResponseDto>> replyToDispute(
            @PathVariable UUID shopId,
            @PathVariable UUID disputeId,
            @Valid @RequestBody MerchantReplyDisputeRequest request,
            @AuthenticationPrincipal User owner) {
        DisputeResponseDto updated = disputeService.replyToDispute(shopId, disputeId, request, owner);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Reply posted successfully"));
    }
}

