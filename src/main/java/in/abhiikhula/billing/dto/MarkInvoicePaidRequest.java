package in.abhiikhula.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Admin request to mark an invoice as paid (BRD FR-909)")
public class MarkInvoicePaidRequest {

    @Schema(description = "UPI UTR or bank reference ID confirmed by admin", example = "UPI/324109841290")
    private String paymentRef;

    @Schema(description = "Admin settlement notes", example = "Verified in Google Pay merchant account")
    private String notes;
}

