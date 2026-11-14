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
import in.abhiikhula.user.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "M5 Accounts & Auth", description = "Passwordless OTP Login & Registration APIs (BRD Section 16, 24)")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @GetMapping("/check-phone/{phone}")
    @Operation(summary = "Check if customer phone is already registered", description = "Checks whether a customer profile exists for this phone")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> checkPhone(@PathVariable String phone) {
        String clean = phone.replaceAll("\\D", "");
        if (clean.length() > 10) clean = clean.substring(clean.length() - 10);
        User user = userRepository.findByPhone(clean).orElse(null);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        if (user != null && !Boolean.TRUE.equals(user.getDeleted())) {
            data.put("registered", true);
            data.put("name", user.getName());
            data.put("age", user.getAge());
            data.put("phone", user.getPhone());
        } else {
            data.put("registered", false);
            data.put("name", null);
            data.put("age", null);
            data.put("phone", clean);
        }
        return ResponseEntity.ok(ApiResponse.ok(data, "Phone registration status checked"));
    }

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

