package in.abhiikhula.platformconfig.dto;

import in.abhiikhula.platformconfig.model.ConfigScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to toggle or set a feature flag state")
public class ToggleFlagRequest {

    @NotNull(message = "Enabled state must be specified")
    @Schema(description = "Whether the feature flag is enabled", example = "true")
    private Boolean enabled;

    @Schema(description = "Flag scope (default: GLOBAL)", example = "GLOBAL")
    private ConfigScope scope;

    @Schema(description = "Scope UUID (e.g. zoneId or shopId if scope is not GLOBAL)")
    private UUID scopeId;

    @Schema(description = "Optional reason or description")
    private String description;
}

