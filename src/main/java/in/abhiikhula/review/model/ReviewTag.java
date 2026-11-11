package in.abhiikhula.review.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Quick feedback chips for service quality (BRD FR-1102)")
public enum ReviewTag {
    ON_TIME,
    BEHAVIOUR,
    FAIR_WAIT
}

