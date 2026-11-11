package in.abhiikhula.auth.controller;

import in.abhiikhula.auth.dto.AuthResponse;
import in.abhiikhula.auth.dto.SendOtpRequest;
import in.abhiikhula.auth.dto.SendOtpResponse;
import in.abhiikhula.auth.dto.VerifyOtpRequest;
import in.abhiikhula.auth.service.AuthService;
import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "M5 Accounts & Auth", description = "Passwordless OTP Login & Registration APIs (BRD Section 16, 24)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-otp")
    @Operation(summary = "Send OTP to mobile number", description = "Generates and sends a 6-digit OTP to the Indian mobile number with 120s expiry")
    public ResponseEntity<ApiResponse<SendOtpResponse>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        SendOtpResponse response = authService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "OTP dispatched successfully"));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP & Authenticate", description = "Verifies 6-digit OTP, auto-registers if new, and returns JWT Bearer token")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Authentication successful"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile", description = "Decodes JWT and fetches current user details")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String token) {
        User user = authService.getCurrentUser(token);
        return ResponseEntity.ok(ApiResponse.ok(user, "User profile fetched"));
    }
}

