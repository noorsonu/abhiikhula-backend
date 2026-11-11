package in.abhiikhula.platformconfig.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.platformconfig.dto.FeatureFlagDto;
import in.abhiikhula.platformconfig.dto.PlatformConfigDto;
import in.abhiikhula.platformconfig.dto.ToggleFlagRequest;
import in.abhiikhula.platformconfig.dto.UpdateConfigRequest;
import in.abhiikhula.platformconfig.model.ConfigScope;
import in.abhiikhula.platformconfig.service.PlatformConfigService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ConfigController.class, AdminConfigController.class})
@AutoConfigureMockMvc(addFilters = false)
class ConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlatformConfigService configService;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(UUID.randomUUID())
                .name("Super Admin")
                .phone("9999999999")
                .role(UserRole.ADMIN)
                .build();

        regularUser = User.builder()
                .id(UUID.randomUUID())
                .name("Regular User")
                .phone("9888888888")
                .role(UserRole.CUSTOMER)
                .build();
    }

    private void authenticateAs(User user) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Public: Get feature flags returns 200 OK")
    void testGetPublicFlags() throws Exception {
        FeatureFlagDto flag = FeatureFlagDto.builder()
                .id(UUID.randomUUID())
                .flagKey("flags.map_view")
                .enabled(false)
                .scope(ConfigScope.GLOBAL)
                .description("Map view toggle")
                .build();

        when(configService.getAllFlags()).thenReturn(List.of(flag));

        mockMvc.perform(get("/api/config/flags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].flagKey").value("flags.map_view"))
                .andExpect(jsonPath("$.data[0].enabled").value(false));

        verify(configService).getAllFlags();
    }

    @Test
    @DisplayName("Public: Get settings returns 200 OK")
    void testGetPublicSettings() throws Exception {
        PlatformConfigDto config = PlatformConfigDto.builder()
                .id(UUID.randomUUID())
                .configKey("hold_window_minutes")
                .configValue("10")
                .scope(ConfigScope.GLOBAL)
                .description("Customer hold window")
                .build();

        when(configService.getAllConfigs()).thenReturn(List.of(config));

        mockMvc.perform(get("/api/config/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].configKey").value("hold_window_minutes"))
                .andExpect(jsonPath("$.data[0].configValue").value("10"));

        verify(configService).getAllConfigs();
    }

    @Test
    @DisplayName("Admin: List configs returns 200 OK for admin")
    void testAdminListConfigs() throws Exception {
        authenticateAs(adminUser);

        PlatformConfigDto config = PlatformConfigDto.builder()
                .id(UUID.randomUUID())
                .configKey("max_active_tokens_user")
                .configValue("3")
                .scope(ConfigScope.GLOBAL)
                .build();

        when(configService.getAllConfigs()).thenReturn(List.of(config));

        mockMvc.perform(get("/api/admin/configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].configKey").value("max_active_tokens_user"));
    }

    @Test
    @DisplayName("Admin: List configs returns 403 Forbidden for non-admin")
    void testAdminListConfigsForbidden() throws Exception {
        authenticateAs(regularUser);

        mockMvc.perform(get("/api/admin/configs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin: Update config returns 200 OK")
    void testAdminUpdateConfig() throws Exception {
        authenticateAs(adminUser);

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setValue("15");

        PlatformConfigDto updated = PlatformConfigDto.builder()
                .id(UUID.randomUUID())
                .configKey("hold_window_minutes")
                .configValue("15")
                .scope(ConfigScope.GLOBAL)
                .build();

        when(configService.updateConfig(eq("hold_window_minutes"), any(UpdateConfigRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/admin/configs/hold_window_minutes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.configValue").value("15"));

        verify(configService).updateConfig(eq("hold_window_minutes"), any(UpdateConfigRequest.class));
    }

    @Test
    @DisplayName("Admin: Toggle feature flag returns 200 OK")
    void testAdminToggleFlag() throws Exception {
        authenticateAs(adminUser);

        ToggleFlagRequest request = new ToggleFlagRequest();
        request.setEnabled(false);

        FeatureFlagDto toggled = FeatureFlagDto.builder()
                .id(UUID.randomUUID())
                .flagKey("flags.walk_in")
                .enabled(false)
                .scope(ConfigScope.GLOBAL)
                .build();

        when(configService.toggleFlag(eq("flags.walk_in"), any(ToggleFlagRequest.class)))
                .thenReturn(toggled);

        mockMvc.perform(patch("/api/admin/flags/flags.walk_in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.flagKey").value("flags.walk_in"))
                .andExpect(jsonPath("$.data.enabled").value(false));

        verify(configService).toggleFlag(eq("flags.walk_in"), any(ToggleFlagRequest.class));
    }
}

