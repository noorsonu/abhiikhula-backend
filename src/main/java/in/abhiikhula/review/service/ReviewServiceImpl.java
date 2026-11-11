package in.abhiikhula.review.service;

import in.abhiikhula.common.exception.BadRequestException;
import in.abhiikhula.common.exception.DuplicateResourceException;
import in.abhiikhula.common.exception.ForbiddenException;
import in.abhiikhula.common.exception.ResourceNotFoundException;
import in.abhiikhula.queue.model.Token;
import in.abhiikhula.queue.model.TokenStatus;
import in.abhiikhula.queue.repository.TokenRepository;
import in.abhiikhula.review.dto.*;
import in.abhiikhula.review.mapper.ReviewMapper;
import in.abhiikhula.review.model.Review;
import in.abhiikhula.review.repository.ReviewRepository;
import in.abhiikhula.shop.model.Shop;
import in.abhiikhula.shop.repository.ShopRepository;
import in.abhiikhula.user.model.User;
import in.abhiikhula.user.model.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final TokenRepository tokenRepository;
    private final ShopRepository shopRepository;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional
    public ReviewDto submitReview(CreateReviewRequest request, User currentUser) {
        Token token = tokenRepository.findById(request.getTokenId())
                .orElseThrow(() -> new ResourceNotFoundException("Token", "id", request.getTokenId()));

        // FR-1101: Only after a served token
        if (token.getStatus() != TokenStatus.COMPLETED) {
            throw new BadRequestException("Review can only be submitted for completed/served tokens (BRD FR-1101). Current status: " + token.getStatus());
        }

        // FR-1106: Linked to token id - 1 review per token
        if (reviewRepository.existsByTokenId(token.getId())) {
            throw new DuplicateResourceException("Review has already been submitted for token " + token.getDisplayToken());
        }

        // If token belonged to a registered user, ensure same user submits
        if (token.getUser() != null && currentUser != null) {
            if (!token.getUser().getId().equals(currentUser.getId())) {
                throw new ForbiddenException("You cannot review a token issued to another user");
            }
        }

        String reviewerName = "Verified Customer";
        if (currentUser != null && currentUser.getName() != null && !currentUser.getName().isBlank()) {
            reviewerName = currentUser.getName();
        } else if (request.getCustomerName() != null && !request.getCustomerName().isBlank()) {
            reviewerName = request.getCustomerName().trim();
        } else if (token.getCustomerName() != null && !token.getCustomerName().isBlank()) {
            reviewerName = token.getCustomerName();
        }

        String tagsStr = reviewMapper.tagListToString(request.getTags());

        Review review = Review.builder()
                .shop(token.getShop())
                .token(token)
                .user(currentUser != null ? currentUser : token.getUser())
                .customerName(reviewerName)
                .stars(request.getStars())
                .text(request.getText() != null ? request.getText().trim() : null)
                .tags(tagsStr)
                .verifiedToken(true)
                .hidden(false)
                .abuseReported(false)
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Review submitted for shop {} on token {} with {} stars", token.getShop().getId(), token.getId(), request.getStars());
        return reviewMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopRatingSummaryDto getShopRatingSummary(UUID shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));

        List<Review> activeReviews = reviewRepository.findByShopIdAndHiddenFalseOrderByCreatedAtDesc(shopId);
        long reviewCount = reviewRepository.countActiveReviewsByShopId(shopId);
        Double avg = reviewRepository.getAverageStarsByShopId(shopId);

        // FR-1105: Average rating and count shown after 3 reviews
        boolean isRatingVisible = reviewCount >= 3;
        Double roundedAvg = null;
        if (isRatingVisible && avg != null) {
            roundedAvg = Math.round(avg * 10.0) / 10.0;
        }

        // FR-1107: Automatic ETA accuracy score
        String etaAccuracy = computeEtaAccuracy(shopId);

        return ShopRatingSummaryDto.builder()
                .shopId(shop.getId())
                .averageRating(roundedAvg)
                .reviewCount(reviewCount)
                .isRatingVisible(isRatingVisible)
                .etaAccuracyScore(etaAccuracy)
                .reviews(reviewMapper.toDtoList(activeReviews))
                .build();
    }

    @Override
    @Transactional
    public ReviewDto replyToReview(UUID shopId, UUID reviewId, OwnerReplyRequest request, User ownerUser) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));

        verifyShopOwner(shop, ownerUser);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if (!review.getShop().getId().equals(shopId)) {
            throw new BadRequestException("Review does not belong to shop " + shop.getName());
        }

        // FR-1103: One owner reply per review
        if (review.getReply() != null && !review.getReply().isBlank()) {
            throw new BadRequestException("A reply has already been submitted for this review (BRD FR-1103)");
        }

        review.setReply(request.getReply().trim());
        review.setReplyAt(Instant.now());
        Review updated = reviewRepository.save(review);
        log.info("Shopkeeper replied to review {} for shop {}", reviewId, shopId);
        return reviewMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void reportAbuse(UUID reviewId, ReportAbuseRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        // FR-1104: Report abuse into the admin queue - Hidden while pending
        review.setAbuseReported(true);
        review.setAbuseReason(request.getReason().trim());
        review.setHidden(true);
        reviewRepository.save(review);
        log.warn("Review {} reported for abuse: {}. Temporarily hidden.", reviewId, request.getReason());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewDto> listReportedReviews(User admin) {
        validateAdmin(admin);
        List<Review> reported = reviewRepository.findByAbuseReportedTrueOrderByCreatedAtDesc();
        return reviewMapper.toDtoList(reported);
    }

    @Override
    @Transactional
    public ReviewDto moderateReview(UUID reviewId, ModerateReviewRequest request, User admin) {
        validateAdmin(admin);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            review.setHidden(false);
            review.setAbuseReported(false);
            log.info("Admin {} approved review {}. Review is now visible.", admin.getPhone(), reviewId);
        } else {
            review.setHidden(true);
            review.setAbuseReported(false);
            log.info("Admin {} hid review {}. Review remains hidden.", admin.getPhone(), reviewId);
        }

        Review saved = reviewRepository.save(review);
        return reviewMapper.toDto(saved);
    }

    private String computeEtaAccuracy(UUID shopId) {
        Instant thirtyDaysAgo = Instant.now().minus(Duration.ofDays(30));
        List<Token> completedTokens = tokenRepository.findByShopIdAndJoinedAtBetween(shopId, thirtyDaysAgo, Instant.now())
                .stream()
                .filter(t -> t.getStatus() == TokenStatus.COMPLETED && t.getServedAt() != null && t.getJoinedAt() != null)
                .toList();

        if (completedTokens.isEmpty()) {
            return "87% accurate"; // Default cold-start baseline (BRD FR-1107)
        }

        long accurateCount = completedTokens.stream().filter(t -> {
            long actualWaitMin = Duration.between(t.getJoinedAt(), t.getServedAt()).toMinutes();
            int estimatedWaitMin = t.getEstimatedWaitMin() != null ? t.getEstimatedWaitMin() : 15;
            long error = Math.abs(actualWaitMin - estimatedWaitMin);
            return error <= 8; // Within median error tolerance (BRD Section 5.4)
        }).count();

        int percentage = (int) Math.round(((double) accurateCount / completedTokens.size()) * 100);
        percentage = Math.max(70, Math.min(98, percentage));
        return percentage + "% accurate";
    }

    private void verifyShopOwner(Shop shop, User user) {
        if (user == null) {
            throw new ForbiddenException("Authentication required to manage shop reviews");
        }
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (shop.getOwner() == null || !shop.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("You are not the registered owner of this shop");
        }
    }

    private void validateAdmin(User admin) {
        if (admin == null || admin.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Admin privileges required to access this resource");
        }
    }
}

