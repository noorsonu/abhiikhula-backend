package in.abhiikhula.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to delete customer account under DPDP Act 2023 (BRD Section 24.3 / FR-507)")
public class DeleteAccountRequest {

    @Size(max = 255, message = "Reason must not exceed 255 characters")
    @Schema(description = "Optional reason for account deletion", example = "Relocating to another city")
    private String reason;

    @AssertTrue(message = "Confirmation must be explicitly true to erase account")
    @Schema(description = "Confirmation checkbox that user consents to permanent deletion of account and active tokens", example = "true")
    private Boolean confirmation;
}

