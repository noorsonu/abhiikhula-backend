package in.abhiikhula.analytics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.analytics.dto.AdminFunnelReportDto;
import in.abhiikhula.analytics.dto.AdminZoneReportDto;
import in.abhiikhula.analytics.dto.RecordEventRequest;
import in.abhiikhula.analytics.dto.ShopPeriodReportDto;
import in.abhiikhula.analytics.service.AnalyticsService;
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
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AnalyticsEventController.class, MerchantAnalyticsController.class, AdminAnalyticsController.class})
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AnalyticsService analyticsService;

    private User merchantUser;
    private User adminUser;
    private UUID shopId;
    private UUID zoneId;

    @BeforeEach
    void setUp() {
        shopId = UUID.randomUUID();
        zoneId = UUID.randomUUID();

        merchantUser = User.builder()
                .id(UUID.randomUUID())
                .name("Salim Merchant")
                .phone("9876543210")
                .role(UserRole.SHOP_OWNER)
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .name("Super Admin")
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
    @DisplayName("POST /api/analytics/events - ingest anonymous engagement event")
    void recordEvent_ShouldSucceed() throws Exception {
        RecordEventRequest req = RecordEventRequest.builder()
                .name("SEARCH_OPEN")
                .deviceId("anon-dev-123")
                .props(Map.of("queryLength", 4))
                .build();

        mockMvc.perform(post("/api/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Event recorded successfully"));

        verify(analyticsService).recordEvent(any(RecordEventRequest.class), any());
    }

    @Test
    @DisplayName("GET /api/shops/{shopId}/analytics/report - get 7-day merchant report")
    void getShopPeriodReport_ShouldReturnReport() throws Exception {
        authenticateAs(merchantUser);

        ShopPeriodReportDto report = ShopPeriodReportDto.builder()
                .shopId(shopId)
                .shopName("Royal Barber")
                .periodDays(7)
                .totalTokens(45L)
                .totalServed(38L)
                .totalNoShows(4L)
                .noShowRatePct(8.9)
                .averageWaitMin(12.4)
                .peakHour("17:00 - 18:00")
                .estimatedLostWalkIns(7L)
                .trendPoints(List.of())
                .build();

        when(analyticsService.getShopPeriodReport(eq(shopId), eq(7), any(User.class)))
                .thenReturn(report);

        mockMvc.perform(get("/api/shops/{shopId}/analytics/report", shopId)
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalTokens").value(45))
                .andExpect(jsonPath("$.data.noShowRatePct").value(8.9))
                .andExpect(jsonPath("$.data.peakHour").value("17:00 - 18:00"));

        verify(analyticsService).getShopPeriodReport(eq(shopId), eq(7), any(User.class));
    }

    @Test
    @DisplayName("GET /api/admin/analytics/zones/{zoneId} - get admin zone health report")
    void getZoneReport_ShouldReturnReport() throws Exception {
        authenticateAs(adminUser);

        AdminZoneReportDto report = AdminZoneReportDto.builder()
                .zoneId(zoneId)
                .zoneName("Indira Nagar")
                .totalShops(12L)
                .liveOpenShops(8L)
                .totalTokensIssued(150L)
                .totalTokensServed(130L)
                .overallCompliancePct(88.5)
                .activeSubscribers(10L)
                .build();

        when(analyticsService.getZoneReport(eq(zoneId), any(User.class)))
                .thenReturn(report);

        mockMvc.perform(get("/api/admin/analytics/zones/{zoneId}", zoneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.zoneName").value("Indira Nagar"))
                .andExpect(jsonPath("$.data.totalShops").value(12));

        verify(analyticsService).getZoneReport(eq(zoneId), any(User.class));
    }

    @Test
    @DisplayName("GET /api/admin/analytics/funnel - get platform conversion funnel")
    void getFunnelReport_ShouldReturnFunnel() throws Exception {
        authenticateAs(adminUser);

        AdminFunnelReportDto funnel = AdminFunnelReportDto.builder()
                .searchViews(500L)
                .detailViews(200L)
                .tokensTaken(80L)
                .checkins(60L)
                .served(55L)
                .viewToTokenConversionPct(40.0)
                .tokenToServeConversionPct(68.8)
                .build();

        when(analyticsService.getFunnelReport(any(), any(), any(), any(User.class)))
                .thenReturn(funnel);

        mockMvc.perform(get("/api/admin/analytics/funnel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.searchViews").value(500))
                .andExpect(jsonPath("$.data.viewToTokenConversionPct").value(40.0));

        verify(analyticsService).getFunnelReport(any(), any(), any(), any(User.class));
    }

    @Test
    @DisplayName("GET /api/shops/{shopId}/analytics/export/tokens.csv - streams token CSV")
    void exportTokensCsv_ShouldReturnCsvContent() throws Exception {
        authenticateAs(merchantUser);

        mockMvc.perform(get("/api/shops/{shopId}/analytics/export/tokens.csv", shopId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/csv")))
                .andExpect(header().exists("Content-Disposition"));

        verify(analyticsService).exportTokensCsv(eq(shopId), any(), any(), any(), any(User.class), any());
    }

    @Test
    @DisplayName("GET /api/admin/analytics/export/shops.csv - streams shops CSV")
    void exportShopsCsv_ShouldReturnCsvContent() throws Exception {
        authenticateAs(adminUser);

        mockMvc.perform(get("/api/admin/analytics/export/shops.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/csv")))
                .andExpect(header().exists("Content-Disposition"));

        verify(analyticsService).exportShopsCsv(any(), any(User.class), any());
    }
}

