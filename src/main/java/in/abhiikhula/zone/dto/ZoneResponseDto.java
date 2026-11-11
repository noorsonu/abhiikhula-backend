package in.abhiikhula.zone.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Hyperlocal Zone Details (BRD Section 29)")
public class ZoneResponseDto {

    @Schema(description = "Unique Identifier of the Zone")
    private UUID id;

    @Schema(description = "Name of the hyperlocal neighbourhood", example = "Indira Nagar (Sector 14)")
    private String name;

    @Schema(description = "City where zone is located", example = "Lucknow")
    private String city;

    @Schema(description = "Center latitude coordinate", example = "26.885400")
    private BigDecimal lat;

    @Schema(description = "Center longitude coordinate", example = "80.994200")
    private BigDecimal lng;

    @Schema(description = "Hyperlocal discovery radius in kilometers", example = "2.0")
    private Double radiusKm;

    @Schema(description = "Whether this zone is actively live on the platform", example = "true")
    private Boolean active;

    @Schema(description = "Timestamp when zone was registered")
    private Instant createdAt;
}

