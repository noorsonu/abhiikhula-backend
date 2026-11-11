package in.abhiikhula.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.auth.dto.AuthResponse;
import in.abhiikhula.auth.dto.SendOtpRequest;
import in.abhiikhula.auth.dto.SendOtpResponse;
import in.abhiikhula.auth.dto.VerifyOtpRequest;
import in.abhiikhula.auth.service.AuthService;
import in.abhiikhula.common.exception.UnauthorizedException;
import in.abhiikhula.user.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/send-otp sends OTP and returns 200")
    void shouldSendOtp() throws Exception {
        SendOtpRequest request = SendOtpRequest.builder()
                .phone("9876543210")
                .build();

        SendOtpResponse response = SendOtpResponse.builder()
                .message("OTP sent successfully to 9876543210")
                .expiresInSeconds(120)
                .build();

        when(authService.sendOtp(any(SendOtpRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(120));
    }

    @Test
    @DisplayName("POST /api/auth/verify-otp returns JWT token on success")
    void shouldVerifyOtpAndReturnToken() throws Exception {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .phone("9876543210")
                .otp("123456")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt.mock.token")
                .tokenType("Bearer")
                .userId(UUID.randomUUID())
                .phone("9876543210")
                .role(UserRole.CUSTOMER)
                .newUser(true)
                .build();

        when(authService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt.mock.token"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.newUser").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/verify-otp returns 401 when OTP is invalid")
    void shouldReturn401WhenOtpInvalid() throws Exception {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .phone("9876543210")
                .otp("000000")
                .build();

        when(authService.verifyOtp(any(VerifyOtpRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid or expired OTP. Please request a new one."));

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid or expired OTP. Please request a new one."));
    }

    @Test
    @DisplayName("POST /api/auth/send-otp returns 400 for invalid phone number")
    void shouldReturn400ForInvalidPhone() throws Exception {
        SendOtpRequest request = SendOtpRequest.builder()
                .phone("123") // invalid format
                .build();

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }
}

