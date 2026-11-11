package in.abhiikhula.billing.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mode of payment for subscription invoices")
public enum PaymentMethod {
    OFFLINE_UPI,
    CASH,
    MANUAL_ADMIN,
    ONLINE
}

