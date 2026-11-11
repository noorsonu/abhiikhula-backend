package in.abhiikhula.queue.dto;

import in.abhiikhula.queue.model.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Live virtual queue metrics for a shop")
public class QueueStatusResponseDto {

    private UUID shopId;
    private String shopName;
    private UUID sessionId;
    private SessionStatus sessionStatus;
    private Integer currentServingTokenNumber;
    private String currentServingDisplayToken;
    private Integer totalTokensIssued;
    private Integer waitingCount;
    private Integer estimatedWaitMin;
    private Integer delayMinutes;
    private String delayReason;
    private java.time.Instant pausedUntil;
    private String pauseReason;
    private boolean acceptingTokens;
}

