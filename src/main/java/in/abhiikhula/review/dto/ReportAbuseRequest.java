package in.abhiikhula.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Customer/Merchant report review abuse request (BRD FR-1104)")
public class ReportAbuseRequest {

    @NotBlank(message = "Abuse reason is required")
    @Schema(description = "Reason for reporting (e.g., abusive language, defamation, fake)", example = "Contains abusive language and profanity")
    private String reason;
}

