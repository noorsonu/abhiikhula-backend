package in.abhiikhula.dispute.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Merchant dispute response (BRD FR-1304)")
public class MerchantReplyDisputeRequest {

    @NotBlank(message = "Reply text is required")
    @Schema(description = "Merchant explanation or apology", example = "Emergency electricity shutdown occurred, apologies for the inconvenience.")
    private String reply;
}

