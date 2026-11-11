package in.abhiikhula.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Admin review moderation decision (BRD FR-1104, AD-11)")
public class ModerateReviewRequest {

    @NotBlank(message = "Action is required (APPROVE or HIDE)")
    @Pattern(regexp = "APPROVE|HIDE", message = "Action must be APPROVE or HIDE")
    @Schema(description = "Moderation action: APPROVE (unhide) or HIDE (keep hidden)", example = "APPROVE")
    private String action;
}

