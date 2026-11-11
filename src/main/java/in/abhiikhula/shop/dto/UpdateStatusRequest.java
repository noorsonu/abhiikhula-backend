package in.abhiikhula.shop.dto;

import in.abhiikhula.shop.model.ShopStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update shop live status and crowd level (BRD Module M2)")
public class UpdateStatusRequest {

    @NotNull(message = "Shop status is required (OPEN, BUSY, PAUSED, CLOSED)")
    @Schema(description = "Current live status", example = "BUSY", allowableValues = {"OPEN", "BUSY", "PAUSED", "CLOSED"})
    private ShopStatus status;

    @NotNull(message = "Crowd level is required (1: Empty, 2: Light, 3: Crowded, 4: Very Crowded)")
    @Min(value = 1, message = "Crowd level must be between 1 and 4")
    @Max(value = 4, message = "Crowd level must be between 1 and 4")
    @Schema(description = "1 = <5m wait, 2 = 5-15m wait, 3 = 15-35m wait, 4 = 35m+ wait", example = "3")
    private Integer crowdLevel;

    @Schema(description = "Optional merchant broadcast note (e.g. 'Lunch till 2:30 PM')", example = "Heavy rush before lunch")
    private String note;

    @Schema(description = "Current device latitude for audit verification", example = "26.886100")
    private BigDecimal lat;

    @Schema(description = "Current device longitude for audit verification", example = "80.995100")
    private BigDecimal lng;

    @Schema(description = "Source of update (default: OWNER_APP)", example = "OWNER_APP")
    @Builder.Default
    private String source = "OWNER_APP";
}

