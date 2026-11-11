package in.abhiikhula.dispute.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard dispute reasons as defined in BRD Annexure C")
public enum DisputeReason {
    WAS_CLOSED,
    WRONG_WAIT,
    BEHAVIOUR,
    TOKEN_CANCELLED,
    SHOP_NOT_FOUND,
    DUPLICATE,
    OTHER
}

