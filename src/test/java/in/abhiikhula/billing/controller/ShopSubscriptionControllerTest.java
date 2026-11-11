package in.abhiikhula.billing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.billing.dto.InvoiceResponseDto;
import in.abhiikhula.billing.dto.SubscriptionResponseDto;
import in.abhiikhula.billing.dto.UpgradeSubscriptionRequest;
import in.abhiikhula.billing.model.InvoiceStatus;
import in.abhiikhula.billing.model.PaymentMethod;
import in.abhiikhula.billing.model.PlanCode;
import in.abhiikhula.billing.model.SubscriptionStatus;
import in.abhiikhula.billing.service.BillingService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ShopSubscriptionController.class, AdminBillingController.class})
@AutoConfigureMockMvc(addFilters = false)
class ShopSubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BillingService billingService;

    private User merchantUser;
    private User adminUser;
    private UUID shopId;

    @BeforeEach
    void setUp() {
        shopId = UUID.randomUUID();

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
    @DisplayName("GET /api/shops/{id}/subscription returns active subscription details")
    void testGetSubscription() throws Exception {
        authenticateAs(merchantUser);

        SubscriptionResponseDto dto = SubscriptionResponseDto.builder()
                .id(UUID.randomUUID())
                .shopId(shopId)
                .shopName("Royal Barber")
                .planCode(PlanCode.LIFETIME_FREE_PRO)
                .status(SubscriptionStatus.ACTIVE)
                .tokensUsedToday(5)
                .dailyTokenLimit(99999)
                .isPaywallActive(false)
                .build();

        when(billingService.getShopSubscription(eq(shopId), any(User.class))).thenReturn(dto);

        mockMvc.perform(get("/api/shops/{shopId}/subscription", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.planCode").value("LIFETIME_FREE_PRO"))
                .andExpect(jsonPath("$.data.isPaywallActive").value(false));

        verify(billingService).getShopSubscription(eq(shopId), any(User.class));
    }

    @Test
    @DisplayName("POST /api/shops/{id}/subscription/upgrade creates pending offline UPI invoice")
    void testRequestUpgrade() throws Exception {
        authenticateAs(merchantUser);

        UpgradeSubscriptionRequest request = UpgradeSubscriptionRequest.builder()
                .targetPlan(PlanCode.LIVE_PRO)
                .paymentMethod(PaymentMethod.OFFLINE_UPI)
                .paymentRef("UPI/123456789")
                .notes("PhonePe payment done")
                .build();

        InvoiceResponseDto invoiceDto = InvoiceResponseDto.builder()
                .id(UUID.randomUUID())
                .shopId(shopId)
                .invoiceNumber("INV-2026-00001")
                .amount(new BigDecimal("399.00"))
                .status(InvoiceStatus.PENDING)
                .paymentMethod(PaymentMethod.OFFLINE_UPI)
                .paymentRef("UPI/123456789")
                .createdAt(Instant.now())
                .build();

        when(billingService.requestPlanUpgrade(eq(shopId), any(UpgradeSubscriptionRequest.class), any(User.class)))
                .thenReturn(invoiceDto);

        mockMvc.perform(post("/api/shops/{shopId}/subscription/upgrade", shopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invoiceNumber").value("INV-2026-00001"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("Admin: List all subscriptions returns 200 OK")
    void testAdminListSubscriptions() throws Exception {
        authenticateAs(adminUser);

        SubscriptionResponseDto dto = SubscriptionResponseDto.builder()
                .id(UUID.randomUUID())
                .shopId(shopId)
                .planCode(PlanCode.FREE)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        when(billingService.listAllSubscriptions(any(User.class))).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].planCode").value("FREE"));
    }
}

