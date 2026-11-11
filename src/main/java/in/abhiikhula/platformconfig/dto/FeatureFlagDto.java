package in.abhiikhula.platformconfig.dto;

import in.abhiikhula.platformconfig.model.ConfigScope;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Feature flag / 1-tap kill switch state")
public class FeatureFlagDto {
    private UUID id;
    private String flagKey;
    private Boolean enabled;
    private ConfigScope scope;
    private UUID scopeId;
    private String description;
    private Instant updatedAt;
}

