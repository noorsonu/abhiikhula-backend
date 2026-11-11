package in.abhiikhula.claim.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.claim.dto.ClaimResponseDto;
import in.abhiikhula.claim.dto.ReviewClaimRequest;
import in.abhiikhula.claim.dto.SubmitClaimRequest;
import in.abhiikhula.claim.model.ClaimStatus;
import in.abhiikhula.claim.model.DocumentType;
import in.abhiikhula.claim.service.ShopClaimService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ShopClaimController.class, AdminClaimController.class})
@AutoConfigureMockMvc(addFilters = false)
class ShopClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShopClaimService shopClaimService;

    private User testUser;
    private User adminUser;
    private UUID shopId;
    private UUID claimId;

    @BeforeEach
    void setUp() {
        shopId = UUID.randomUUID();
        claimId = UUID.randomUUID();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .phone("9876543210")
                .name("Ramesh Kumar")
                .role(UserRole.CUSTOMER)
                .language("hi")
                .blocked(false)
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .phone("9999999999")
                .name("Admin User")
                .role(UserRole.ADMIN)
                .language("en")
                .blocked(false)
                .build();

        var auth = new UsernamePasswordAuthenticationToken(testUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("POST /api/shops/{shopId}/claim - Returns 201 Created")
    void submitClaim_Success() throws Exception {
        SubmitClaimRequest request = SubmitClaimRequest.builder()
                .documentType(DocumentType.SHOP_PHOTO_WITH_BANNER)
                .documentUrl("https://storage.abhiikhula.in/photos/banner.jpg")
                .applicantPhone("9876543210")
                .build();

        ClaimResponseDto dto = ClaimResponseDto.builder()
                .id(claimId)
                .shopId(shopId)
                .shopName("Royal Barber")
                .applicantId(testUser.getId())
                .applicantName("Ramesh Kumar")
                .applicantPhone("9876543210")
                .documentType(DocumentType.SHOP_PHOTO_WITH_BANNER)
                .documentUrl("https://storage.abhiikhula.in/photos/banner.jpg")
                .status(ClaimStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(shopClaimService.submitClaim(eq(shopId), any(SubmitClaimRequest.class), any(User.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/shops/{shopId}/claim", shopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.documentType").value("SHOP_PHOTO_WITH_BANNER"));

        verify(shopClaimService).submitClaim(eq(shopId), any(SubmitClaimRequest.class), any(User.class));
    }

    @Test
    @DisplayName("POST /api/shops/{shopId}/claim - Returns 400 when invalid phone")
    void submitClaim_ValidationError() throws Exception {
        SubmitClaimRequest request = SubmitClaimRequest.builder()
                .documentType(DocumentType.SHOP_PHOTO_WITH_BANNER)
                .documentUrl("https://storage.abhiikhula.in/photos/banner.jpg")
                .applicantPhone("12345") // invalid 5 digit
                .build();

        mockMvc.perform(post("/api/shops/{shopId}/claim", shopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("GET /api/claims/my - Returns 200 with applicant's claims")
    void getMyClaims_Success() throws Exception {
        ClaimResponseDto dto = ClaimResponseDto.builder()
                .id(claimId)
                .shopId(shopId)
                .shopName("Royal Barber")
                .status(ClaimStatus.PENDING)
                .build();

        when(shopClaimService.getMyClaims(any(User.class))).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/claims/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].shopName").value("Royal Barber"));

        verify(shopClaimService).getMyClaims(any(User.class));
    }

    @Test
    @DisplayName("GET /api/admin/claims - Returns 200 with pending claims")
    void adminGetClaims_Success() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(adminUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        ClaimResponseDto dto = ClaimResponseDto.builder()
                .id(claimId)
                .shopId(shopId)
                .shopName("Royal Barber")
                .status(ClaimStatus.PENDING)
                .build();

        when(shopClaimService.getClaimsByStatus(eq(ClaimStatus.PENDING), any(User.class)))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/claims?status=PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        verify(shopClaimService).getClaimsByStatus(eq(ClaimStatus.PENDING), any(User.class));
    }

    @Test
    @DisplayName("PATCH /api/admin/claims/{claimId}/review - Returns 200 with APPROVED status")
    void adminReviewClaim_Approved() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(adminUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        ReviewClaimRequest request = ReviewClaimRequest.builder()
                .status(ClaimStatus.APPROVED)
                .adminNotes("Verified phone number and shop banner")
                .build();

        ClaimResponseDto dto = ClaimResponseDto.builder()
                .id(claimId)
                .shopId(shopId)
                .shopName("Royal Barber")
                .status(ClaimStatus.APPROVED)
                .adminNotes("Verified phone number and shop banner")
                .build();

        when(shopClaimService.reviewClaim(eq(claimId), any(ReviewClaimRequest.class), any(User.class)))
                .thenReturn(dto);

        mockMvc.perform(patch("/api/admin/claims/{claimId}/review", claimId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(shopClaimService).reviewClaim(eq(claimId), any(ReviewClaimRequest.class), any(User.class));
    }
}

