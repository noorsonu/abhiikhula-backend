package in.abhiikhula.shop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.common.exception.ForbiddenException;
import in.abhiikhula.common.exception.ResourceNotFoundException;
import in.abhiikhula.shop.dto.ShopStatusResponseDto;
import in.abhiikhula.shop.dto.StatusEventHistoryDto;
import in.abhiikhula.shop.dto.UpdateStatusRequest;
import in.abhiikhula.shop.model.ShopStatus;
import in.abhiikhula.shop.service.ShopStatusService;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShopStatusController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShopStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShopStatusService shopStatusService;

    private User mockOwner;
    private UUID shopId;

    @BeforeEach
    void setUp() {
        shopId = UUID.randomUUID();
        mockOwner = User.builder()
                .id(UUID.randomUUID())
                .phone("9876543210")
                .role(UserRole.SHOP_OWNER)
                .build();

        var auth = new UsernamePasswordAuthenticationToken(mockOwner, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("PUT /api/shops/{id}/status updates live status and crowd level")
    void shouldUpdateShopStatusSuccessfully() throws Exception {
        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .status(ShopStatus.BUSY)
                .crowdLevel(3)
                .note("Rush before lunch")
                .lat(new BigDecimal("26.886100"))
                .lng(new BigDecimal("80.995100"))
                .build();

        ShopStatusResponseDto responseDto = ShopStatusResponseDto.builder()
                .shopId(shopId)
                .shopName("Royal Barber")
                .status(ShopStatus.BUSY)
                .crowdLevel(3)
                .crowdLabel("Crowded (15–35 min wait)")
                .note("Rush before lunch")
                .lastStatusUpdateAt(Instant.now())
                .recentEvents(List.of(
                        StatusEventHistoryDto.builder()
                                .id(UUID.randomUUID())
                                .fromStatus(ShopStatus.OPEN)
                                .toStatus(ShopStatus.BUSY)
                                .crowdLevel(3)
                                .crowdLabel("Crowded (15–35 min wait)")
                                .source("OWNER_APP")
                                .note("Rush before lunch")
                                .createdAt(Instant.now())
                                .build()
                ))
                .build();

        when(shopStatusService.updateStatus(eq(shopId), any(UpdateStatusRequest.class), any(User.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/api/shops/{shopId}/status", shopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("BUSY"))
                .andExpect(jsonPath("$.data.crowdLevel").value(3))
                .andExpect(jsonPath("$.data.crowdLabel").value("Crowded (15–35 min wait)"))
                .andExpect(jsonPath("$.data.recentEvents").isArray());
    }

    @Test
    @DisplayName("PUT /api/shops/{id}/status returns 403 when user is not the shop owner")
    void shouldReturn403WhenUserIsNotOwner() throws Exception {
        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .status(ShopStatus.CLOSED)
                .crowdLevel(1)
                .build();

        when(shopStatusService.updateStatus(eq(shopId), any(UpdateStatusRequest.class), any(User.class)))
                .thenThrow(new ForbiddenException("You are not authorized to update status for this shop"));

        mockMvc.perform(put("/api/shops/{shopId}/status", shopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("You are not authorized to update status for this shop"));
    }

    @Test
    @DisplayName("PUT /api/shops/{id}/status returns 404 when shop does not exist")
    void shouldReturn404WhenShopNotFound() throws Exception {
        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .status(ShopStatus.OPEN)
                .crowdLevel(1)
                .build();

        when(shopStatusService.updateStatus(eq(shopId), any(UpdateStatusRequest.class), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Shop", "id", shopId));

        mockMvc.perform(put("/api/shops/{shopId}/status", shopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT /api/shops/{id}/status returns 400 when crowdLevel is out of range 1-4")
    void shouldReturn400WhenCrowdLevelInvalid() throws Exception {
        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .status(ShopStatus.OPEN)
                .crowdLevel(5) // Invalid! Allowed: 1-4
                .build();

        mockMvc.perform(put("/api/shops/{shopId}/status", shopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.crowdLevel").exists());
    }

    @Test
    @DisplayName("GET /api/shops/{id}/status returns public live status and timeline")
    void shouldGetLiveStatusSuccessfully() throws Exception {
        ShopStatusResponseDto responseDto = ShopStatusResponseDto.builder()
                .shopId(shopId)
                .shopName("Gupta Medical Store")
                .status(ShopStatus.OPEN)
                .crowdLevel(1)
                .crowdLabel("Empty (< 5 min wait)")
                .lastStatusUpdateAt(Instant.now())
                .recentEvents(Collections.emptyList())
                .build();

        when(shopStatusService.getStatus(eq(shopId))).thenReturn(responseDto);

        mockMvc.perform(get("/api/shops/{shopId}/status", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.crowdLevel").value(1))
                .andExpect(jsonPath("$.data.crowdLabel").value("Empty (< 5 min wait)"));
    }
}

