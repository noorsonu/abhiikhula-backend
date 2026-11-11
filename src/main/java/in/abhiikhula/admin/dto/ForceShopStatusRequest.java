package in.abhiikhula.admin.dto;

import in.abhiikhula.shop.model.ShopStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Admin override to force change shop status (BRD FR-804)")
public class ForceShopStatusRequest {

    @NotNull(message = "Shop status is required (OPEN, CLOSED, BUSY, PAUSED)")
    @Schema(description = "Forced status", example = "CLOSED")
    private ShopStatus status;

    @Min(value = 1, message = "Crowd level must be between 1 and 4")
    @Max(value = 4, message = "Crowd level must be between 1 and 4")
    @Schema(description = "Optional crowd level override (1-4)", example = "1")
    private Integer crowdLevel;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    @Schema(description = "Admin reason for forced status override", example = "Health inspection order / emergency curfew")
    private String reason;
}

