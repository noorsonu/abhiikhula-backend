package in.abhiikhula.claim.dto;

import in.abhiikhula.claim.model.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to claim an unverified or existing shop listing (BRD FR-601, FR-603)")
public class SubmitClaimRequest {

    @NotNull(message = "Document type is required")
    @Schema(description = "Verification document type", example = "SHOP_PHOTO_WITH_BANNER")
    private DocumentType documentType;

    @NotBlank(message = "Document URL is required")
    @Schema(description = "URL of uploaded photo or proof document", example = "https://storage.abhiikhula.in/claims/banner_photo.jpg")
    private String documentUrl;

    @NotBlank(message = "Applicant phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
    @Schema(description = "Contact phone number for verification", example = "9876543210")
    private String applicantPhone;
}

