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
@Schema(description = "Platform configuration key-value representation")
public class PlatformConfigDto {
    private UUID id;
    private String configKey;
    private String configValue;
    private ConfigScope scope;
    private UUID scopeId;
    private String description;
    private Instant updatedAt;
}

