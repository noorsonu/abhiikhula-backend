package in.abhiikhula.review.controller;

import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.review.dto.OwnerReplyRequest;
import in.abhiikhula.review.dto.ReviewDto;
import in.abhiikhula.review.service.ReviewService;
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
@RequestMapping("/api/shops/{shopId}/reviews")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "M11 Reviews & Trust - Merchant", description = "Shopkeeper public reply to customer reviews (BRD Section 22, FR-1103)")
public class MerchantReviewController {

    private final ReviewService reviewService;

    @PostMapping("/{reviewId}/reply")
    @Operation(summary = "Shopkeeper reply to review", description = "Allows shop owner to submit one official public reply (BRD FR-1103)")
    public ResponseEntity<ApiResponse<ReviewDto>> replyToReview(
            @PathVariable UUID shopId,
            @PathVariable UUID reviewId,
            @Valid @RequestBody OwnerReplyRequest request,
            @AuthenticationPrincipal User owner) {
        ReviewDto updatedReview = reviewService.replyToReview(shopId, reviewId, request, owner);
        return ResponseEntity.ok(ApiResponse.ok(updatedReview, "Reply posted successfully"));
    }
}

