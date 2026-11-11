package in.abhiikhula.queue.dto;

import in.abhiikhula.queue.model.TokenStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Customer's digital queue token details")
public class TokenResponseDto {

    private UUID id;
    private UUID shopId;
    private String shopName;
    private UUID sessionId;
    private Integer tokenNumber;
    private String displayToken;
    private TokenStatus status;
    private String source;
    private String customerName;
    private String customerPhone;
    private Integer estimatedWaitMin;
    private Integer delayMinutes;
    private String delayReason;
    private Integer peopleAhead;
    private Instant joinedAt;
    private Instant calledAt;
    private Boolean checkedIn;
    private Instant checkedInAt;
    private Boolean requeued;
    private UUID groupId;
    private Integer groupSize;
    private Integer groupMemberIndex;
}

