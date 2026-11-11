package in.abhiikhula.claim.dto;

import in.abhiikhula.claim.model.ClaimStatus;
import in.abhiikhula.claim.model.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Details of shop claim submission and review status (BRD Section 17)")
public class ClaimResponseDto {

    @Schema(description = "Claim unique ID")
    private UUID id;

    @Schema(description = "Shop unique ID")
    private UUID shopId;

    @Schema(description = "Shop name", example = "Royal Barber & Hair Style")
    private String shopName;

    @Schema(description = "Applicant user ID")
    private UUID applicantId;

    @Schema(description = "Applicant user name", example = "Ramesh Kumar")
    private String applicantName;

    @Schema(description = "Applicant phone number", example = "9876543210")
    private String applicantPhone;

    @Schema(description = "Verification document type")
    private DocumentType documentType;

    @Schema(description = "Document photo or file URL")
    private String documentUrl;

    @Schema(description = "Claim status (PENDING, APPROVED, REJECTED)")
    private ClaimStatus status;

    @Schema(description = "Rejection reason if rejected")
    private String rejectionReason;

    @Schema(description = "Admin notes")
    private String adminNotes;

    @Schema(description = "Reviewing admin user ID")
    private UUID reviewedByAdminId;

    @Schema(description = "Timestamp when reviewed")
    private Instant reviewedAt;

    @Schema(description = "Timestamp when claim was submitted")
    private Instant createdAt;
}

