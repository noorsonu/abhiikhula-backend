package in.abhiikhula.dispute.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dispute resolution actions by admin (BRD FR-1305, FR-806)")
public enum ResolutionAction {
    WARN,
    STRIKE,
    CREDIT,
    DISMISS
}

