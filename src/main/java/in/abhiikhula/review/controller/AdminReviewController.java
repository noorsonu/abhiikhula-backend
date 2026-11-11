package in.abhiikhula.review.controller;

import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.review.dto.ModerateReviewRequest;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "M11 Reviews & Trust - Admin", description = "Admin review moderation queue and abuse resolution (BRD Section 22, FR-1104, AD-11)")
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping("/abuse-queue")
    @Operation(summary = "Admin: List abuse-reported reviews", description = "Returns reviews flagged for moderation (BRD FR-1104)")
    public ResponseEntity<ApiResponse<List<ReviewDto>>> listReportedReviews(@AuthenticationPrincipal User admin) {
        List<ReviewDto> list = reviewService.listReportedReviews(admin);
        return ResponseEntity.ok(ApiResponse.ok(list, "Reported reviews fetched successfully"));
    }

    @PatchMapping("/{reviewId}/moderate")
    @Operation(summary = "Admin: Moderate review", description = "Approve (unhide) or Hide review permanently (BRD FR-1104, AD-11)")
    public ResponseEntity<ApiResponse<ReviewDto>> moderateReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ModerateReviewRequest request,
            @AuthenticationPrincipal User admin) {
        ReviewDto moderated = reviewService.moderateReview(reviewId, request, admin);
        return ResponseEntity.ok(ApiResponse.ok(moderated, "Review moderated successfully"));
    }
}

