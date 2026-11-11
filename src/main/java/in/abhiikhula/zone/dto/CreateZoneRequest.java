package in.abhiikhula.zone.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload to register a new Hyperlocal Zone")
public class CreateZoneRequest {

    @NotBlank(message = "Zone name is mandatory")
    @Schema(description = "Zone name (Neighbourhood)", example = "Indira Nagar (Sector 14)")
    private String name;

    @NotBlank(message = "City is mandatory")
    @Schema(description = "City name", example = "Lucknow")
    private String city;

    @NotNull(message = "Latitude is mandatory")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    @Schema(description = "Center latitude", example = "26.885400")
    private BigDecimal lat;

    @NotNull(message = "Longitude is mandatory")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
    @Schema(description = "Center longitude", example = "80.994200")
    private BigDecimal lng;

    @Schema(description = "Discovery radius in km (default 2.0, max 5.0 in Phase 1)", example = "2.0")
    @Builder.Default
    private Double radiusKm = 2.0;

    @Schema(description = "Launch status of this zone", example = "true")
    @Builder.Default
    private Boolean active = true;
}

