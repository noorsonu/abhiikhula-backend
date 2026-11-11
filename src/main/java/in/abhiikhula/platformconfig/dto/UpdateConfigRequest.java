package in.abhiikhula.platformconfig.dto;

import in.abhiikhula.platformconfig.model.ConfigScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update or create a platform configuration")
public class UpdateConfigRequest {

    @NotBlank(message = "Config value must not be blank")
    @Schema(description = "New configuration value", example = "15")
    private String value;

    @Schema(description = "Configuration scope (default: GLOBAL)", example = "GLOBAL")
    private ConfigScope scope;

    @Schema(description = "Scope UUID (e.g. zoneId or shopId if scope is not GLOBAL)")
    private UUID scopeId;

    @Schema(description = "Optional description")
    private String description;
}

