package in.abhiikhula.dispute.dto;

import in.abhiikhula.dispute.model.ResolutionAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Admin dispute adjudication decision (BRD FR-1305, FR-806)")
public class ResolveDisputeRequest {

    @NotNull(message = "Resolution action is required")
    @Schema(description = "Action taken: WARN, STRIKE, CREDIT, DISMISS", example = "STRIKE")
    private ResolutionAction action;

    @Schema(description = "Admin remarks explaining the verdict", example = "Status was 45 min stale. 1 Strike issued to shopkeeper.")
    private String notes;
}

