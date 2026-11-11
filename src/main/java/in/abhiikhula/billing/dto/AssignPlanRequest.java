package in.abhiikhula.billing.dto;

import in.abhiikhula.billing.model.PlanCode;
import in.abhiikhula.billing.model.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Admin request to assign or override a shop subscription plan (BRD FR-901)")
public class AssignPlanRequest {

    @NotNull(message = "Plan code is required")
    @Schema(description = "Plan to assign (FREE, LIVE_PRO, LIVE_PRO_PLUS, LIFETIME_FREE_PRO)", example = "LIFETIME_FREE_PRO")
    private PlanCode planCode;

    @Schema(description = "Subscription status (default: ACTIVE)", example = "ACTIVE")
    private SubscriptionStatus status;

    @Schema(description = "Duration in days (null for indefinite / lifetime)", example = "30")
    private Integer durationDays;

    @Schema(description = "Administrative reason for plan assignment", example = "Pilot founder partner grant")
    private String reason;
}

