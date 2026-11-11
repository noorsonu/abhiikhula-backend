package in.abhiikhula.queue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Customer Geo Check-in within 100m of shop (BRD FR-307)")
public class CheckInRequest {

    @NotNull(message = "Latitude is required for geo check-in")
    @DecimalMin(value = "-90.0", message = "Invalid latitude")
    @DecimalMax(value = "90.0", message = "Invalid latitude")
    @Schema(description = "Customer's live latitude at shop door", example = "26.886000")
    private BigDecimal lat;

    @NotNull(message = "Longitude is required for geo check-in")
    @DecimalMin(value = "-180.0", message = "Invalid longitude")
    @DecimalMax(value = "180.0", message = "Invalid longitude")
    @Schema(description = "Customer's live longitude at shop door", example = "80.995000")
    private BigDecimal lng;
}

