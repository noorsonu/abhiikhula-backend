package in.abhiikhula.dispute.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dispute ticket lifecycle states (BRD FR-1303)")
public enum DisputeState {
    NEW,
    IN_REVIEW,
    RESOLVED,
    CLOSED
}

