package in.abhiikhula.claim.dto;

import in.abhiikhula.claim.model.ClaimStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Admin review request for shop claim (BRD FR-604, FR-605)")
public class ReviewClaimRequest {

    @NotNull(message = "Review status is required (APPROVED or REJECTED)")
    @Schema(description = "Decision status: APPROVED or REJECTED", example = "APPROVED")
    private ClaimStatus status;

    @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
    @Schema(description = "Mandatory explanation if status is REJECTED", example = "Banner photo does not match shop name on listing")
    private String rejectionReason;

    @Size(max = 500, message = "Admin notes must not exceed 500 characters")
    @Schema(description = "Internal notes by admin", example = "Verified with electricity bill matching address")
    private String adminNotes;
}

