package in.abhiikhula.ads.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.ads.dto.BookFeaturedSlotRequest;
import in.abhiikhula.ads.dto.FeaturedSlotDto;
import in.abhiikhula.ads.dto.SlotAvailabilityDto;
import in.abhiikhula.ads.service.FeaturedSlotService;
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
import java.time.LocalDate;
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

@WebMvcTest({FeaturedSlotController.class, MerchantFeaturedSlotController.class, AdminFeaturedSlotController.class})
@AutoConfigureMockMvc(addFilters = false)
class FeaturedSlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeaturedSlotService featuredSlotService;

    private User merchantUser;
    private User adminUser;
    private UUID shopId;
    private UUID zoneId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        shopId = UUID.randomUUID();
        zoneId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        merchantUser = User.builder()
                .id(UUID.randomUUID())
                .name("Ramesh Gupta")
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
    @DisplayName("GET /api/ads/featured/availability - should return availability list")
    void checkAvailability_ShouldReturnSlots() throws Exception {
        LocalDate targetDate = LocalDate.now().plusDays(1);
        SlotAvailabilityDto availability = SlotAvailabilityDto.builder()
                .date(targetDate)
                .availablePositions(List.of(1, 2))
                .fullyBooked(false)
                .build();

        when(featuredSlotService.getSlotAvailability(zoneId, categoryId, targetDate))
                .thenReturn(availability);

        mockMvc.perform(get("/api/ads/featured/availability")
                        .param("zoneId", zoneId.toString())
                        .param("categoryId", categoryId.toString())
                        .param("date", targetDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullyBooked").value(false))
                .andExpect(jsonPath("$.data.availablePositions[0]").value(1))
                .andExpect(jsonPath("$.data.availablePositions[1]").value(2));

        verify(featuredSlotService).getSlotAvailability(zoneId, categoryId, targetDate);
    }

    @Test
    @DisplayName("POST /api/ads/featured/{slotId}/click - should record click")
    void recordClick_ShouldSucceed() throws Exception {
        UUID slotId = UUID.randomUUID();

        mockMvc.perform(post("/api/ads/featured/{slotId}/click", slotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Click recorded"));

        verify(featuredSlotService).recordClick(slotId);
    }

    @Test
    @DisplayName("POST /api/shops/{shopId}/featured-slots/book - merchant books sponsored slot")
    void bookSlot_ShouldReturnBookedSlot() throws Exception {
        authenticateAs(merchantUser);

        LocalDate slotDate = LocalDate.now().plusDays(1);
        BookFeaturedSlotRequest req = BookFeaturedSlotRequest.builder()
                .shopId(shopId)
                .slotDate(slotDate)
                .preferredPosition(1)
                .build();

        FeaturedSlotDto responseDto = FeaturedSlotDto.builder()
                .id(UUID.randomUUID())
                .shopId(shopId)
                .shopName("Gupta Kirana")
                .zoneId(zoneId)
                .categoryId(categoryId)
                .slotDate(slotDate)
                .position(1)
                .impressions(0)
                .clicks(0)
                .active(true)
                .createdAt(Instant.now())
                .build();

        when(featuredSlotService.bookFeaturedSlot(any(BookFeaturedSlotRequest.class), any(User.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/shops/{shopId}/featured-slots/book", shopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shopName").value("Gupta Kirana"))
                .andExpect(jsonPath("$.data.position").value(1))
                .andExpect(jsonPath("$.data.active").value(true));

        verify(featuredSlotService).bookFeaturedSlot(any(BookFeaturedSlotRequest.class), any(User.class));
    }

    @Test
    @DisplayName("GET /api/shops/{shopId}/featured-slots - merchant retrieves booked slots")
    void getShopBookedSlots_ShouldReturnList() throws Exception {
        authenticateAs(merchantUser);

        FeaturedSlotDto slotDto = FeaturedSlotDto.builder()
                .id(UUID.randomUUID())
                .shopId(shopId)
                .shopName("Gupta Kirana")
                .zoneId(zoneId)
                .categoryId(categoryId)
                .slotDate(LocalDate.now())
                .position(1)
                .impressions(42)
                .clicks(5)
                .active(true)
                .build();

        when(featuredSlotService.getShopBookedSlots(eq(shopId), any(User.class)))
                .thenReturn(List.of(slotDto));

        mockMvc.perform(get("/api/shops/{shopId}/featured-slots", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].impressions").value(42))
                .andExpect(jsonPath("$.data[0].clicks").value(5));

        verify(featuredSlotService).getShopBookedSlots(eq(shopId), any(User.class));
    }

    @Test
    @DisplayName("GET /api/admin/featured-slots - admin retrieves all platform slots")
    void adminListSlots_ShouldReturnSlots() throws Exception {
        authenticateAs(adminUser);

        FeaturedSlotDto slotDto = FeaturedSlotDto.builder()
                .id(UUID.randomUUID())
                .shopId(shopId)
                .shopName("Gupta Kirana")
                .zoneId(zoneId)
                .categoryId(categoryId)
                .slotDate(LocalDate.now())
                .position(1)
                .impressions(100)
                .clicks(12)
                .active(true)
                .build();

        when(featuredSlotService.listAllSlots(any(), any(), any(User.class)))
                .thenReturn(List.of(slotDto));

        mockMvc.perform(get("/api/admin/featured-slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].shopName").value("Gupta Kirana"))
                .andExpect(jsonPath("$.data[0].impressions").value(100));

        verify(featuredSlotService).listAllSlots(any(), any(), any(User.class));
    }
}
