package in.abhiikhula.shop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.common.exception.ResourceNotFoundException;
import in.abhiikhula.shop.dto.CreateShopRequest;
import in.abhiikhula.shop.dto.ShopDetailDto;
import in.abhiikhula.shop.dto.ShopSummaryDto;
import in.abhiikhula.shop.model.ShopStatus;
import in.abhiikhula.shop.service.ShopService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShopController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShopControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShopService shopService;

    private static final UUID ZONE_ID     = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID SHOP_ID     = UUID.randomUUID();

    private ShopSummaryDto buildSummary(ShopStatus status, int crowd) {
        return ShopSummaryDto.builder()
                .id(SHOP_ID)
                .name("Royal Barber & Hair Style")
                .slug("royal-barber-indira-nagar")
                .categoryName("Salon & Barber")
                .categoryIcon("scissors")
                .status(status)
                .crowdLevel(crowd)
                .crowdLabel("Light (5–15 min wait)")
                .lat(new BigDecimal("26.886100"))
                .lng(new BigDecimal("80.995100"))
                .phone("9876543210")
                .verified(true)
                .distanceKm(0.12)
                .distanceDisplay("120 m")
                .lastStatusUpdateAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/shops?zoneId= returns shops list")
    void shouldReturnShopListForZone() throws Exception {
        when(shopService.discoverShops(eq(ZONE_ID), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(buildSummary(ShopStatus.OPEN, 2)));

        mockMvc.perform(get("/api/shops")
                        .param("zoneId", ZONE_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data[0].crowdLevel").value(2));
    }

    @Test
    @DisplayName("GET /api/shops with openOnly=true filters closed shops")
    void shouldReturnShopsFilteredByOpenOnly() throws Exception {
        when(shopService.discoverShops(eq(ZONE_ID), eq(CATEGORY_ID), eq(true), eq("barber"), any(), any(), any(), any()))
                .thenReturn(List.of(buildSummary(ShopStatus.OPEN, 2)));

        mockMvc.perform(get("/api/shops")
                        .param("zoneId", ZONE_ID.toString())
                        .param("categoryId", CATEGORY_ID.toString())
                        .param("openOnly", "true")
                        .param("q", "barber")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data[0].categoryName").value("Salon & Barber"));
    }

    @Test
    @DisplayName("GET /api/shops with lat, lng and sortBy=DISTANCE returns distance info")
    void shouldReturnShopsWithDistanceWhenCoordinatesProvided() throws Exception {
        when(shopService.discoverShops(any(), any(), any(), any(), eq(26.8860), eq(80.9950), any(), eq("DISTANCE")))
                .thenReturn(List.of(buildSummary(ShopStatus.OPEN, 2)));

        mockMvc.perform(get("/api/shops")
                        .param("lat", "26.8860")
                        .param("lng", "80.9950")
                        .param("sortBy", "DISTANCE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].distanceKm").value(0.12))
                .andExpect(jsonPath("$.data[0].distanceDisplay").value("120 m"));
    }

    @Test
    @DisplayName("GET /api/shops without zoneId or coordinates returns 400 Bad Request")
    void shouldReturn400WhenNeitherZoneNorCoordinatesProvided() throws Exception {
        mockMvc.perform(get("/api/shops")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/shops/{id} returns shop detail")
    void shouldReturnShopDetail() throws Exception {
        ShopDetailDto detail = ShopDetailDto.builder()
                .id(SHOP_ID)
                .zoneId(ZONE_ID)
                .zoneName("Indira Nagar (Sector 14)")
                .categoryId(CATEGORY_ID)
                .categoryName("Salon & Barber")
                .categoryIcon("scissors")
                .defaultServiceMin(15)
                .name("Royal Barber & Hair Style")
                .slug("royal-barber-indira-nagar")
                .address("Shop No. 12, Indira Nagar, Lucknow")
                .lat(new BigDecimal("26.886100"))
                .lng(new BigDecimal("80.995100"))
                .phone("9876543210")
                .status(ShopStatus.OPEN)
                .crowdLevel(2)
                .crowdLabel("Light (5–15 min wait)")
                .tokenCap(50)
                .verified(true)
                .lastStatusUpdateAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        when(shopService.getShopById(SHOP_ID)).thenReturn(detail);

        mockMvc.perform(get("/api/shops/{id}", SHOP_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Royal Barber & Hair Style"))
                .andExpect(jsonPath("$.data.defaultServiceMin").value(15))
                .andExpect(jsonPath("$.data.tokenCap").value(50));
    }

    @Test
    @DisplayName("POST /api/shops creates shop and returns 201 Created")
    void shouldCreateShop() throws Exception {
        CreateShopRequest request = CreateShopRequest.builder()
                .zoneId(ZONE_ID)
                .categoryId(CATEGORY_ID)
                .name("New Salon")
                .address("Sector 14, Indira Nagar")
                .lat(new BigDecimal("26.885000"))
                .lng(new BigDecimal("80.994000"))
                .phone("9876543299")
                .tokenCap(80)
                .build();

        ShopDetailDto created = ShopDetailDto.builder()
                .id(UUID.randomUUID())
                .zoneId(ZONE_ID)
                .categoryId(CATEGORY_ID)
                .name(request.getName())
                .slug("new-salon")
                .address(request.getAddress())
                .lat(request.getLat())
                .lng(request.getLng())
                .phone(request.getPhone())
                .status(ShopStatus.CLOSED)
                .crowdLevel(1)
                .tokenCap(request.getTokenCap())
                .verified(false)
                .createdAt(Instant.now())
                .build();

        when(shopService.createShop(any(CreateShopRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/shops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Salon"));
    }

    @Test
    @DisplayName("GET /api/shops/{id} returns 404 for unknown shop")
    void shouldReturn404ForUnknownShop() throws Exception {
        UUID unknown = UUID.randomUUID();
        when(shopService.getShopById(unknown))
                .thenThrow(new ResourceNotFoundException("Shop", "id", unknown));

        mockMvc.perform(get("/api/shops/{id}", unknown)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
