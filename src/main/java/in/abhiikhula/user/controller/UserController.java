package in.abhiikhula.user.controller;

import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.common.exception.UnauthorizedException;
import in.abhiikhula.user.dto.DeleteAccountRequest;
import in.abhiikhula.user.dto.UpdateUserProfileRequest;
import in.abhiikhula.user.dto.UserProfileResponseDto;
import in.abhiikhula.user.model.User;
import in.abhiikhula.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "M5 Accounts & Profiles", description = "Customer/Merchant Profile Management & DPDP Compliance (BRD Section 16 & Section 24)")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns profile details of the authenticated user including home zone (BRD Section 16)")
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> getProfile(
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        UserProfileResponseDto profile = userService.getProfile(currentUser);
        return ResponseEntity.ok(ApiResponse.ok(profile, "Profile fetched successfully"));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update current user profile", description = "Updates name, email, preferred language, or home zone (BRD Section 16 / FR-502)")
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> updateProfile(
            @Valid @RequestBody UpdateUserProfileRequest request,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        UserProfileResponseDto updatedProfile = userService.updateProfile(currentUser, request);
        return ResponseEntity.ok(ApiResponse.ok(updatedProfile, "Profile updated successfully"));
    }

    @PostMapping("/me/delete-account")
    @Operation(summary = "Delete user account (DPDP Right to Erasure)", description = "Permanently anonymizes user PII, cancels active queue tokens, and purges device tokens under DPDP Act 2023 (BRD Section 24.3 / FR-507)")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        userService.deleteAccount(currentUser, request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Account successfully erased and anonymized under DPDP Act 2023"));
    }
}

