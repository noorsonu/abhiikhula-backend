package in.abhiikhula.dispute.dto;

import in.abhiikhula.dispute.model.DisputeReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Customer dispute ticket creation request (BRD FR-1301)")
public class CreateDisputeRequest {

    @NotNull(message = "Shop ID is required")
    @Schema(description = "Target shop UUID", example = "d1b2c3d4-0000-0000-0000-000000000001")
    private UUID shopId;

    @Schema(description = "Optional related token UUID", example = "e1b2c3d4-0000-0000-0000-000000000001")
    private UUID tokenId;

    @NotNull(message = "Dispute reason is required")
    @Schema(description = "Category of dispute", example = "WAS_CLOSED")
    private DisputeReason reason;

    @Schema(description = "Customer explanation of the issue", example = "Arrived at 10:30 PM, shutter was closed despite status showing OPEN.")
    private String description;

    @Schema(description = "Contact phone for guest customers", example = "9123456780")
    private String customerPhone;
}

