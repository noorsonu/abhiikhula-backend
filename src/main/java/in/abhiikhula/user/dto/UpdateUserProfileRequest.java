package in.abhiikhula.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update user profile (BRD Section 16 / FR-502)")
public class UpdateUserProfileRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Schema(description = "User display name", example = "Amit Verma")
    private String name;

    @Email(message = "Enter a valid email address")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    @Schema(description = "User email address", example = "amit.verma@example.com")
    private String email;

    @Pattern(regexp = "^(en|hi|ur)$", message = "Language must be 'en', 'hi', or 'ur'")
    @Schema(description = "Preferred language code", example = "hi")
    private String language;

    @Schema(description = "Preferred / Home zone UUID", example = "a0b1c2d3-0000-0000-0000-000000000001")
    private UUID preferredZoneId;
}

