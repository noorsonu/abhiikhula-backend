package in.abhiikhula.queue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request by shopkeeper to issue an offline / walk-in customer token (BRD FR-704)")
public class IssueWalkInTokenRequest {

    @Schema(description = "Customer name or identifier (optional)", example = "Sharma Ji")
    @Size(max = 100, message = "Customer name must not exceed 100 characters")
    private String customerName;

    @Schema(description = "Customer phone number (optional)", example = "9876543210")
    @Size(max = 20, message = "Customer phone must not exceed 20 characters")
    private String customerPhone;

    @Schema(description = "Optional service notes / instructions", example = "Haircut only")
    private String note;
}

