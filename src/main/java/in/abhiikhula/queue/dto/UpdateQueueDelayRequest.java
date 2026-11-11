package in.abhiikhula.queue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update/announce queue delay by merchant (BRD Section 15)")
public class UpdateQueueDelayRequest {

    @NotNull(message = "Delay minutes is required")
    @Min(value = 0, message = "Delay minutes cannot be negative")
    @Max(value = 180, message = "Delay minutes cannot exceed 180 minutes")
    @Schema(description = "Extra delay minutes added to estimated wait time (0 to clear delay)", example = "15")
    private Integer delayMinutes;

    @Schema(description = "Optional reason or note for delay shown to customers", example = "Hair spa and styling running 15 min extra")
    private String reason;
}

