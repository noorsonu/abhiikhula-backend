package in.abhiikhula.zone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.zone.dto.CreateZoneRequest;
import in.abhiikhula.zone.dto.ZoneResponseDto;
import in.abhiikhula.zone.service.ZoneService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ZoneController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class ZoneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ZoneService zoneService;

    @Test
    @DisplayName("GET /api/zones returns active zones with standard ApiResponse wrapper")
    void shouldReturnActiveZones() throws Exception {
        UUID zoneId = UUID.randomUUID();
        ZoneResponseDto zoneDto = ZoneResponseDto.builder()
                .id(zoneId)
                .name("Indira Nagar (Sector 14)")
                .city("Lucknow")
                .lat(new BigDecimal("26.885400"))
                .lng(new BigDecimal("80.994200"))
                .radiusKm(2.0)
                .active(true)
                .createdAt(Instant.now())
                .build();

        when(zoneService.getAllActiveZones()).thenReturn(List.of(zoneDto));

        mockMvc.perform(get("/api/zones")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Indira Nagar (Sector 14)"))
                .andExpect(jsonPath("$.data[0].city").value("Lucknow"))
                .andExpect(jsonPath("$.data[0].radiusKm").value(2.0));
    }

    @Test
    @DisplayName("POST /api/zones creates zone and returns 201 Created")
    void shouldCreateZone() throws Exception {
        CreateZoneRequest request = CreateZoneRequest.builder()
                .name("Indira Nagar (Sector 14)")
                .city("Lucknow")
                .lat(new BigDecimal("26.885400"))
                .lng(new BigDecimal("80.994200"))
                .radiusKm(2.0)
                .active(true)
                .build();

        ZoneResponseDto createdDto = ZoneResponseDto.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .city(request.getCity())
                .lat(request.getLat())
                .lng(request.getLng())
                .radiusKm(request.getRadiusKm())
                .active(request.getActive())
                .createdAt(Instant.now())
                .build();

        when(zoneService.createZone(any(CreateZoneRequest.class))).thenReturn(createdDto);

        mockMvc.perform(post("/api/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Indira Nagar (Sector 14)"));
    }
}

