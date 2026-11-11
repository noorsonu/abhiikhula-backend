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
@Schema(description = "Merchant request to pause or resume queue (BRD FR-707)")
public class PauseQueueRequest {

    @NotNull(message = "Pause minutes is required")
    @Min(value = 5, message = "Pause duration must be at least 5 minutes")
    @Max(value = 180, message = "Pause duration cannot exceed 180 minutes (3 hours)")
    @Schema(description = "Minutes to pause queue (e.g. 15, 30, 60)", example = "30")
    private Integer pauseMinutes;

    @Schema(description = "Optional reason shown to waiting customers", example = "Lunch break / prayer")
    private String reason;
}

