package in.abhiikhula.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.notification.dto.NotificationResponseDto;
import in.abhiikhula.notification.dto.RegisterDeviceRequest;
import in.abhiikhula.notification.model.NotificationChannel;
import in.abhiikhula.notification.model.NotificationTemplate;
import in.abhiikhula.notification.service.NotificationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    private User testUser;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .phone("9876543211")
                .role(UserRole.CUSTOMER)
                .build();

        notificationId = UUID.randomUUID();

        var auth = new UsernamePasswordAuthenticationToken(testUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("POST /api/notifications/devices registers device successfully")
    void shouldRegisterDeviceSuccessfully() throws Exception {
        RegisterDeviceRequest request = RegisterDeviceRequest.builder()
                .fcmToken("fcm_token_test_1234")
                .deviceType("WEB")
                .userAgent("Chrome on Windows")
                .build();

        mockMvc.perform(post("/api/notifications/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Device registered successfully"));

        verify(notificationService).registerDevice(any(RegisterDeviceRequest.class), any(User.class));
    }

    @Test
    @DisplayName("GET /api/notifications/my returns notification list")
    void shouldGetMyNotifications() throws Exception {
        NotificationResponseDto dto = NotificationResponseDto.builder()
                .id(notificationId)
                .shopName("Royal Barber")
                .template(NotificationTemplate.TWO_AHEAD)
                .channel(NotificationChannel.IN_APP)
                .title("Ghar se nikalne ka samay!")
                .body("Royal Barber: leave in 10 min — token #5, two ahead.")
                .read(false)
                .sentAt(Instant.now())
                .build();

        when(notificationService.getMyNotifications(any(User.class))).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/notifications/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].template").value("TWO_AHEAD"))
                .andExpect(jsonPath("$.data[0].title").value("Ghar se nikalne ka samay!"))
                .andExpect(jsonPath("$.data[0].read").value(false));
    }

    @Test
    @DisplayName("GET /api/notifications/unread-count returns unread count")
    void shouldGetUnreadCount() throws Exception {
        when(notificationService.getUnreadCount(any(User.class))).thenReturn(3L);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read marks notification as read")
    void shouldMarkNotificationAsRead() throws Exception {
        NotificationResponseDto dto = NotificationResponseDto.builder()
                .id(notificationId)
                .read(true)
                .build();

        when(notificationService.markAsRead(eq(notificationId), any(User.class))).thenReturn(dto);

        mockMvc.perform(patch("/api/notifications/{id}/read", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.read").value(true));
    }

    @Test
    @DisplayName("POST /api/notifications/read-all marks all notifications as read")
    void shouldMarkAllAsRead() throws Exception {
        mockMvc.perform(post("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("All notifications marked as read"));

        verify(notificationService).markAllAsRead(any(User.class));
    }
}

