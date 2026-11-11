package in.abhiikhula.review.controller;

import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.review.dto.CreateReviewRequest;
import in.abhiikhula.review.dto.ReportAbuseRequest;
import in.abhiikhula.review.dto.ReviewDto;
import in.abhiikhula.review.dto.ShopRatingSummaryDto;
import in.abhiikhula.review.service.ReviewService;
import in.abhiikhula.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "M11 Reviews & Trust - Public", description = "Verified post-service ratings and shop review feed (BRD Section 22)")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/reviews")
    @Operation(summary = "Submit post-service review", description = "Submits a verified 1-5 star review linked to a completed token (BRD FR-1101, FR-1106)")
    public ResponseEntity<ApiResponse<ReviewDto>> submitReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal User currentUser) {
        ReviewDto review = reviewService.submitReview(request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(review, "Review submitted successfully"));
    }

    @GetMapping("/api/shops/{shopId}/reviews")
    @Operation(summary = "Get shop reviews and trust score", description = "Returns visible customer reviews and aggregated star rating once 3+ reviews exist (BRD FR-1105, FR-1107)")
    public ResponseEntity<ApiResponse<ShopRatingSummaryDto>> getShopReviews(@PathVariable UUID shopId) {
        ShopRatingSummaryDto summary = reviewService.getShopRatingSummary(shopId);
        return ResponseEntity.ok(ApiResponse.ok(summary, "Shop reviews fetched successfully"));
    }

    @PostMapping("/api/reviews/{reviewId}/report-abuse")
    @Operation(summary = "Report review abuse", description = "Flags a review for admin moderation and temporarily hides it (BRD FR-1104)")
    public ResponseEntity<ApiResponse<Void>> reportAbuse(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReportAbuseRequest request) {
        reviewService.reportAbuse(reviewId, request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Review reported for moderation"));
    }
}

