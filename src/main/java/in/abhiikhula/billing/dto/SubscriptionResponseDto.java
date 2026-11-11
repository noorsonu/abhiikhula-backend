package in.abhiikhula.billing.dto;

import in.abhiikhula.billing.model.PlanCode;
import in.abhiikhula.billing.model.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Shop subscription status and quota details")
public class SubscriptionResponseDto {
    private UUID id;
    private UUID shopId;
    private String shopName;
    private PlanCode planCode;
    private SubscriptionStatus status;
    private Instant trialEndsAt;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private Integer tokensUsedToday;
    private Integer dailyTokenLimit;
    private Boolean autoRenew;
    private Boolean isPaywallActive;
}

