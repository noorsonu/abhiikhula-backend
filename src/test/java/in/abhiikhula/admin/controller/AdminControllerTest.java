package in.abhiikhula.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.admin.dto.AdminShopModerationDto;
import in.abhiikhula.admin.dto.AdminUserModerationDto;
import in.abhiikhula.admin.dto.ForceShopStatusRequest;
import in.abhiikhula.admin.dto.PlatformMetricsDto;
import in.abhiikhula.admin.service.AdminGovernanceService;
import in.abhiikhula.common.exception.ForbiddenException;
import in.abhiikhula.shop.model.ShopStatus;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminGovernanceService adminGovernanceService;

    private User adminUser;
    private UUID shopId;
    private UUID targetUserId;

    @BeforeEach
    void setUp() {
        shopId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .phone("9999999999")
                .name("Super Admin")
                .role(UserRole.ADMIN)
                .language("en")
                .blocked(false)
                .build();

        var auth = new UsernamePasswordAuthenticationToken(adminUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GET /api/admin/metrics - Returns 200 with platform metrics")
    void getPlatformMetrics_Success() throws Exception {
        PlatformMetricsDto metrics = PlatformMetricsDto.builder()
                .totalUsers(1200)
                .totalCustomers(1130)
                .totalShopOwners(70)
                .blockedUsers(2)
                .totalShops(85)
                .verifiedShops(50)
                .unverifiedShops(35)
                .activeShops(80)
                .suspendedShops(5)
                .activeQueuesNow(12)
                .tokensIssuedToday(350)
                .activeZones(3)
                .activeCategories(8)
                .build();

        when(adminGovernanceService.getPlatformMetrics(any(User.class))).thenReturn(metrics);

        mockMvc.perform(get("/api/admin/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(1200))
                .andExpect(jsonPath("$.data.totalShops").value(85))
                .andExpect(jsonPath("$.data.activeQueuesNow").value(12))
                .andExpect(jsonPath("$.data.tokensIssuedToday").value(350));

        verify(adminGovernanceService).getPlatformMetrics(any(User.class));
    }

    @Test
    @DisplayName("GET /api/admin/metrics - Returns 401 when unauthenticated")
    void getPlatformMetrics_Unauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/admin/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/admin/shops/{shopId}/toggle-active - Returns 200 with moderation info")
    void toggleShopActive_Success() throws Exception {
        AdminShopModerationDto dto = AdminShopModerationDto.builder()
                .shopId(shopId)
                .name("Gupta Medical Store")
                .status(ShopStatus.CLOSED)
                .active(false)
                .verified(true)
                .message("Shop suspended by admin governance")
                .build();

        when(adminGovernanceService.toggleShopActive(eq(shopId), any(User.class))).thenReturn(dto);

        mockMvc.perform(patch("/api/admin/shops/{shopId}/toggle-active", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.message").value("Shop suspended by admin governance"));

        verify(adminGovernanceService).toggleShopActive(eq(shopId), any(User.class));
    }

    @Test
    @DisplayName("PATCH /api/admin/shops/{shopId}/force-status - Returns 200 with new status")
    void forceShopStatus_Success() throws Exception {
        ForceShopStatusRequest request = ForceShopStatusRequest.builder()
                .status(ShopStatus.CLOSED)
                .crowdLevel(1)
                .reason("Health inspection emergency")
                .build();

        AdminShopModerationDto dto = AdminShopModerationDto.builder()
                .shopId(shopId)
                .name("Gupta Medical Store")
                .status(ShopStatus.CLOSED)
                .active(true)
                .verified(true)
                .message("Shop status force-updated to CLOSED")
                .build();

        when(adminGovernanceService.forceShopStatus(eq(shopId), any(ForceShopStatusRequest.class), any(User.class))).thenReturn(dto);

        mockMvc.perform(patch("/api/admin/shops/{shopId}/force-status", shopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        verify(adminGovernanceService).forceShopStatus(eq(shopId), any(ForceShopStatusRequest.class), any(User.class));
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{userId}/block - Returns 200 on user block")
    void blockUser_Success() throws Exception {
        AdminUserModerationDto dto = AdminUserModerationDto.builder()
                .userId(targetUserId)
                .phone("9876543219")
                .name("Spammer")
                .role(UserRole.CUSTOMER)
                .blocked(true)
                .message("User blocked successfully and active tokens cancelled.")
                .build();

        when(adminGovernanceService.blockUser(eq(targetUserId), any(User.class))).thenReturn(dto);

        mockMvc.perform(patch("/api/admin/users/{userId}/block", targetUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.blocked").value(true));

        verify(adminGovernanceService).blockUser(eq(targetUserId), any(User.class));
    }

    @Test
    @DisplayName("GET /api/admin/metrics - Returns 403 when non-admin requests metrics")
    void adminEndpoint_ForbiddenForCustomer() throws Exception {
        when(adminGovernanceService.getPlatformMetrics(any(User.class)))
                .thenThrow(new ForbiddenException("Only administrators have access to this resource."));

        mockMvc.perform(get("/api/admin/metrics"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }
}

