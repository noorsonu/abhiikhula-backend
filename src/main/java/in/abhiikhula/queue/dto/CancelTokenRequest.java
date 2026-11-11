package in.abhiikhula.queue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to cancel a token")
public class CancelTokenRequest {

    @Schema(description = "Cancellation reason", example = "Change of plans")
    private String reason;
}

