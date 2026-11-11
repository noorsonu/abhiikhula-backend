package in.abhiikhula.billing.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lifecycle status of a shop subscription")
public enum SubscriptionStatus {
    ACTIVE,
    TRIAL,
    GRACE,
    SUSPENDED,
    CANCELLED
}

