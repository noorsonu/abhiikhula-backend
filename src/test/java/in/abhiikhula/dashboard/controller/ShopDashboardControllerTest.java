package in.abhiikhula.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.common.exception.ForbiddenException;
import in.abhiikhula.dashboard.dto.DailyShopSummaryDto;
import in.abhiikhula.dashboard.dto.MerchantShopDashboardDto;
import in.abhiikhula.dashboard.service.ShopDashboardService;
import in.abhiikhula.shop.model.ShopStatus;
import in.abhiikhula.user.model.User;
import in.abhiikhula.user.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShopDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShopDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShopDashboardService shopDashboardService;

    private User merchantUser;
    private UUID shopId;

    @BeforeEach
    void setUp() {
        shopId = UUID.randomUUID();
        merchantUser = User.builder()
                .id(UUID.randomUUID())
                .phone("9876543210")
                .name("Ramesh Kumar")
                .role(UserRole.SHOP_OWNER)
                .language("hi")
                .blocked(false)
                .build();

        var auth = new UsernamePasswordAuthenticationToken(merchantUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GET /api/shops/my - Returns 200 with merchant owned shops")
    void getMyShops_Success() throws Exception {
        MerchantShopDashboardDto dto = MerchantShopDashboardDto.builder()
                .id(shopId)
                .name("Royal Barber & Hair Style")
                .slug("royal-barber-indira-nagar")
                .categoryName("Salon & Barber")
                .status(ShopStatus.OPEN)
                .crowdLevel(2)
                .crowdLabel("Moderate (5–15 min wait)")
                .activeSessionId(UUID.randomUUID())
                .servingTokenNumber(5)
                .waitingCount(3L)
                .tokenCap(100)
                .verified(true)
                .build();

        when(shopDashboardService.getMyShops(any(User.class))).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/shops/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Royal Barber & Hair Style"))
                .andExpect(jsonPath("$.data[0].servingTokenNumber").value(5))
                .andExpect(jsonPath("$.data[0].waitingCount").value(3));

        verify(shopDashboardService).getMyShops(any(User.class));
    }

    @Test
    @DisplayName("GET /api/shops/my - Returns 401 when unauthenticated")
    void getMyShops_Unauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/shops/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/shops/{shopId}/analytics/daily - Returns 200 with daily summary")
    void getDailySummary_Success() throws Exception {
        LocalDate date = LocalDate.now();
        DailyShopSummaryDto summary = DailyShopSummaryDto.builder()
                .shopId(shopId)
                .shopName("Royal Barber & Hair Style")
                .date(date)
                .totalTokensIssued(45)
                .totalTokensCompleted(38)
                .totalNoShows(4)
                .totalCancelled(2)
                .totalSkipped(1)
                .averageWaitTimeMinutes(18.5)
                .averageServiceTimeMinutes(22.0)
                .peakHour("17:00 - 18:00")
                .totalOperationalHours(8.5)
                .build();

        when(shopDashboardService.getDailySummary(eq(shopId), eq(date), any(User.class))).thenReturn(summary);

        mockMvc.perform(get("/api/shops/{shopId}/analytics/daily", shopId)
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalTokensIssued").value(45))
                .andExpect(jsonPath("$.data.totalTokensCompleted").value(38))
                .andExpect(jsonPath("$.data.peakHour").value("17:00 - 18:00"))
                .andExpect(jsonPath("$.data.averageWaitTimeMinutes").value(18.5));

        verify(shopDashboardService).getDailySummary(eq(shopId), eq(date), any(User.class));
    }

    @Test
    @DisplayName("GET /api/shops/{shopId}/analytics/daily - Returns 403 when not shop owner")
    void getDailySummary_Forbidden() throws Exception {
        when(shopDashboardService.getDailySummary(eq(shopId), any(), any(User.class)))
                .thenThrow(new ForbiddenException("You do not have access to view analytics for this shop."));

        mockMvc.perform(get("/api/shops/{shopId}/analytics/daily", shopId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }
}

