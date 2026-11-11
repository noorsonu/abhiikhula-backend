package in.abhiikhula.auth.dto;

import in.abhiikhula.user.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "JWT Auth response returned after OTP verification")
public class AuthResponse {

    @Schema(description = "JWT Bearer token")
    private String token;

    @Schema(description = "Token type", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "User UUID")
    private UUID userId;

    @Schema(description = "User phone", example = "9876543210")
    private String phone;

    @Schema(description = "User role", example = "SHOP_OWNER")
    private UserRole role;

    @Schema(description = "Whether this was a fresh registration", example = "true")
    private boolean newUser;
}

