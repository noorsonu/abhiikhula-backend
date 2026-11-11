package in.abhiikhula.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.user.dto.DeleteAccountRequest;
import in.abhiikhula.user.dto.UpdateUserProfileRequest;
import in.abhiikhula.user.dto.UserProfileResponseDto;
import in.abhiikhula.user.model.User;
import in.abhiikhula.user.model.UserRole;
import in.abhiikhula.user.service.UserService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private User testUser;
    private UUID userId;
    private UUID zoneId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        zoneId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .phone("9876543211")
                .name("Amit Verma")
                .role(UserRole.CUSTOMER)
                .language("hi")
                .blocked(false)
                .build();

        var auth = new UsernamePasswordAuthenticationToken(testUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GET /api/users/me - Returns 200 with user profile")
    void getProfile_Success() throws Exception {
        UserProfileResponseDto dto = UserProfileResponseDto.builder()
                .id(userId)
                .phone("9876543211")
                .name("Amit Verma")
                .role(UserRole.CUSTOMER)
                .language("hi")
                .preferredZoneId(zoneId)
                .preferredZoneName("Indira Nagar")
                .createdAt(Instant.now())
                .build();

        when(userService.getProfile(any(User.class))).thenReturn(dto);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Amit Verma"))
                .andExpect(jsonPath("$.data.phone").value("9876543211"))
                .andExpect(jsonPath("$.data.preferredZoneName").value("Indira Nagar"));

        verify(userService).getProfile(any(User.class));
    }

    @Test
    @DisplayName("GET /api/users/me - Returns 401 when unauthenticated")
    void getProfile_Unauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/users/me - Returns 200 with updated profile")
    void updateProfile_Success() throws Exception {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .name("Amit Kumar Verma")
                .email("amit@example.com")
                .language("en")
                .preferredZoneId(zoneId)
                .build();

        UserProfileResponseDto updatedDto = UserProfileResponseDto.builder()
                .id(userId)
                .phone("9876543211")
                .name("Amit Kumar Verma")
                .email("amit@example.com")
                .language("en")
                .preferredZoneId(zoneId)
                .preferredZoneName("Indira Nagar")
                .createdAt(Instant.now())
                .build();

        when(userService.updateProfile(any(User.class), any(UpdateUserProfileRequest.class))).thenReturn(updatedDto);

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Amit Kumar Verma"))
                .andExpect(jsonPath("$.data.email").value("amit@example.com"))
                .andExpect(jsonPath("$.data.language").value("en"));

        verify(userService).updateProfile(any(User.class), any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/users/me - Returns 400 when invalid email provided")
    void updateProfile_InvalidEmail() throws Exception {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .email("not-an-email")
                .build();

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST /api/users/me/delete-account - Returns 200 on valid erasure request")
    void deleteAccount_Success() throws Exception {
        DeleteAccountRequest request = DeleteAccountRequest.builder()
                .reason("No longer using service")
                .confirmation(true)
                .build();

        mockMvc.perform(post("/api/users/me/delete-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account successfully erased and anonymized under DPDP Act 2023"));

        verify(userService).deleteAccount(any(User.class), any(DeleteAccountRequest.class));
    }

    @Test
    @DisplayName("POST /api/users/me/delete-account - Returns 400 when confirmation is false")
    void deleteAccount_MissingConfirmation() throws Exception {
        DeleteAccountRequest request = DeleteAccountRequest.builder()
                .reason("Just testing")
                .confirmation(false)
                .build();

        mockMvc.perform(post("/api/users/me/delete-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
