package in.abhiikhula.billing.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payment and settlement status of an invoice")
public enum InvoiceStatus {
    PENDING,
    PAID,
    REFUNDED,
    FAILED
}

