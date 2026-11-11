package in.abhiikhula.admin.dto;

import in.abhiikhula.user.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User account moderation details for admin panel (BRD FR-806)")
public class AdminUserModerationDto {

    @Schema(description = "User unique ID")
    private UUID userId;

    @Schema(description = "User registered phone number")
    private String phone;

    @Schema(description = "User display name")
    private String name;

    @Schema(description = "Platform role")
    private UserRole role;

    @Schema(description = "Whether user account is blocked")
    private Boolean blocked;

    @Schema(description = "Whether account was deleted under DPDP")
    private Boolean deleted;

    @Schema(description = "Moderation status message")
    private String message;
}

