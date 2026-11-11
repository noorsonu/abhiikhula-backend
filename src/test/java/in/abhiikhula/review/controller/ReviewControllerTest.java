package in.abhiikhula.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.review.dto.*;
import in.abhiikhula.review.model.ReviewTag;
import in.abhiikhula.review.service.ReviewService;
import in.abhiikhula.user.model.User;
import in.abhiikhula.user.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ReviewController.class, MerchantReviewController.class, AdminReviewController.class})
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    private User customerUser;
    private User merchantUser;
    private User adminUser;
    private UUID shopId;
    private UUID tokenId;
    private UUID reviewId;

    @BeforeEach
    void setUp() {
        shopId = UUID.randomUUID();
        tokenId = UUID.randomUUID();
        reviewId = UUID.randomUUID();

        customerUser = User.builder()
                .id(UUID.randomUUID())
                .name("Pooja Sharma")
                .phone("9123456780")
                .role(UserRole.CUSTOMER)
                .build();

        merchantUser = User.builder()
                .id(UUID.randomUUID())
                .name("Salim Barber")
                .phone("9876543210")
                .role(UserRole.SHOP_OWNER)
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .name("Platform Admin")
                .phone("9999999999")
                .role(UserRole.ADMIN)
                .build();
    }

    private void authenticateAs(User user) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("POST /api/reviews - submit verified review successfully")
    void submitReview_ShouldSucceed() throws Exception {
        authenticateAs(customerUser);

        CreateReviewRequest request = CreateReviewRequest.builder()
                .tokenId(tokenId)
                .stars(5)
                .text("Excellent haircut and very fair waiting line.")
                .tags(List.of(ReviewTag.ON_TIME, ReviewTag.FAIR_WAIT))
                .build();

        ReviewDto responseDto = ReviewDto.builder()
                .id(reviewId)
                .shopId(shopId)
                .tokenId(tokenId)
                .displayToken("T-07")
                .customerName("Pooja Sharma")
                .stars(5)
                .text("Excellent haircut and very fair waiting line.")
                .tags(List.of(ReviewTag.ON_TIME, ReviewTag.FAIR_WAIT))
                .verifiedToken(true)
                .hidden(false)
                .abuseReported(false)
                .createdAt(Instant.now())
                .build();

        when(reviewService.submitReview(any(CreateReviewRequest.class), any(User.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stars").value(5))
                .andExpect(jsonPath("$.data.verifiedToken").value(true))
                .andExpect(jsonPath("$.data.customerName").value("Pooja Sharma"));

        verify(reviewService).submitReview(any(CreateReviewRequest.class), any(User.class));
    }

    @Test
    @DisplayName("GET /api/shops/{shopId}/reviews - get public reviews and trust rating")
    void getShopReviews_ShouldReturnSummary() throws Exception {
        ShopRatingSummaryDto summary = ShopRatingSummaryDto.builder()
                .shopId(shopId)
                .averageRating(4.8)
                .reviewCount(5L)
                .isRatingVisible(true)
                .etaAccuracyScore("92% accurate")
                .reviews(List.of())
                .build();

        when(reviewService.getShopRatingSummary(shopId)).thenReturn(summary);

        mockMvc.perform(get("/api/shops/{shopId}/reviews", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.averageRating").value(4.8))
                .andExpect(jsonPath("$.data.isRatingVisible").value(true))
                .andExpect(jsonPath("$.data.etaAccuracyScore").value("92% accurate"));

        verify(reviewService).getShopRatingSummary(shopId);
    }

    @Test
    @DisplayName("POST /api/reviews/{reviewId}/report-abuse - flags review for moderation")
    void reportAbuse_ShouldSucceed() throws Exception {
        ReportAbuseRequest req = ReportAbuseRequest.builder()
                .reason("Defamatory remarks")
                .build();

        mockMvc.perform(post("/api/reviews/{reviewId}/report-abuse", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Review reported for moderation"));

        verify(reviewService).reportAbuse(eq(reviewId), any(ReportAbuseRequest.class));
    }

    @Test
    @DisplayName("POST /api/shops/{shopId}/reviews/{reviewId}/reply - merchant replies to review")
    void replyToReview_ShouldSucceed() throws Exception {
        authenticateAs(merchantUser);

        OwnerReplyRequest req = OwnerReplyRequest.builder()
                .reply("Thank you Pooja! Always happy to serve.")
                .build();

        ReviewDto updatedDto = ReviewDto.builder()
                .id(reviewId)
                .shopId(shopId)
                .reply("Thank you Pooja! Always happy to serve.")
                .replyAt(Instant.now())
                .build();

        when(reviewService.replyToReview(eq(shopId), eq(reviewId), any(OwnerReplyRequest.class), any(User.class)))
                .thenReturn(updatedDto);

        mockMvc.perform(post("/api/shops/{shopId}/reviews/{reviewId}/reply", shopId, reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reply").value("Thank you Pooja! Always happy to serve."));

        verify(reviewService).replyToReview(eq(shopId), eq(reviewId), any(OwnerReplyRequest.class), any(User.class));
    }

    @Test
    @DisplayName("GET /api/admin/reviews/abuse-queue - admin retrieves flagged reviews")
    void listReportedReviews_ShouldReturnReported() throws Exception {
        authenticateAs(adminUser);

        ReviewDto dto = ReviewDto.builder()
                .id(reviewId)
                .abuseReported(true)
                .hidden(true)
                .build();

        when(reviewService.listReportedReviews(any(User.class)))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/reviews/abuse-queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].abuseReported").value(true));

        verify(reviewService).listReportedReviews(any(User.class));
    }

    @Test
    @DisplayName("PATCH /api/admin/reviews/{reviewId}/moderate - admin moderates review")
    void moderateReview_ShouldSucceed() throws Exception {
        authenticateAs(adminUser);

        ModerateReviewRequest req = ModerateReviewRequest.builder()
                .action("APPROVE")
                .build();

        ReviewDto moderatedDto = ReviewDto.builder()
                .id(reviewId)
                .hidden(false)
                .abuseReported(false)
                .build();

        when(reviewService.moderateReview(eq(reviewId), any(ModerateReviewRequest.class), any(User.class)))
                .thenReturn(moderatedDto);

        mockMvc.perform(patch("/api/admin/reviews/{reviewId}/moderate", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hidden").value(false));

        verify(reviewService).moderateReview(eq(reviewId), any(ModerateReviewRequest.class), any(User.class));
    }
}

