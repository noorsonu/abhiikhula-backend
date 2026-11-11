package in.abhiikhula.billing.dto;

import in.abhiikhula.billing.model.PaymentMethod;
import in.abhiikhula.billing.model.PlanCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to initiate plan upgrade with offline UPI payment")
public class UpgradeSubscriptionRequest {

    @NotNull(message = "Target plan code is required")
    @Schema(description = "Plan to upgrade to (LIVE_PRO, LIVE_PRO_PLUS)", example = "LIVE_PRO")
    private PlanCode targetPlan;

    @Schema(description = "Payment mode (default: OFFLINE_UPI)", example = "OFFLINE_UPI")
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.OFFLINE_UPI;

    @Schema(description = "Optional UPI UTR / Transaction reference number", example = "UPI/324109841290")
    private String paymentRef;

    @Schema(description = "Optional notes or message from merchant", example = "Paid via PhonePe QR")
    private String notes;
}

