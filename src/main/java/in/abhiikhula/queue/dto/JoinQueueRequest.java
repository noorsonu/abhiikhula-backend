package in.abhiikhula.queue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to join shop's virtual queue (BRD Module M3)")
public class JoinQueueRequest {

    @Schema(description = "Customer display name", example = "Amit Sharma")
    private String customerName;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
    @Schema(description = "Customer 10-digit mobile number (optional if authenticated)", example = "9876543210")
    private String customerPhone;

    @Schema(description = "Entry source (APP, QR_WALKIN, MERCHANT_ENTRY)", example = "APP")
    @Builder.Default
    private String source = "APP";

    @jakarta.validation.constraints.Min(value = 1, message = "Group size must be at least 1")
    @jakarta.validation.constraints.Max(value = 10, message = "Group size cannot exceed 10 per booking")
    @Schema(description = "Number of persons in group (1 to 10)", example = "3")
    @Builder.Default
    private Integer groupSize = 1;

    @Schema(description = "Optional list of companion names", example = "[\"Rahul\", \"Aman\", \"Rohit\"]")
    private java.util.List<String> memberNames;
}

