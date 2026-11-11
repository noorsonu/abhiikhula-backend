package in.abhiikhula.billing.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Subscription plan tier (BRD Section 20)")
public enum PlanCode {
    FREE,
    LIVE_PRO,
    LIVE_PRO_PLUS,
    LIFETIME_FREE_PRO
}

