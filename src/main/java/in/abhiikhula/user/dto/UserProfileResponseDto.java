package in.abhiikhula.user.dto;

import in.abhiikhula.user.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Customer/Merchant Profile details (BRD Section 16 & 24)")
public class UserProfileResponseDto {

    @Schema(description = "User unique ID")
    private UUID id;

    @Schema(description = "Registered mobile number", example = "9876543210")
    private String phone;

    @Schema(description = "Display name", example = "Amit Verma")
    private String name;

    @Schema(description = "Optional email address", example = "amit.verma@example.com")
    private String email;

    @Schema(description = "Role in platform", example = "CUSTOMER")
    private UserRole role;

    @Schema(description = "Preferred language (hi, en, ur)", example = "hi")
    private String language;

    @Schema(description = "Home / preferred zone ID")
    private UUID preferredZoneId;

    @Schema(description = "Home / preferred zone name", example = "Indira Nagar")
    private String preferredZoneName;

    @Schema(description = "Account registration timestamp")
    private Instant createdAt;
}

