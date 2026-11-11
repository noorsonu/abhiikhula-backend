package in.abhiikhula.dispute.dto;

import in.abhiikhula.dispute.model.DisputeReason;
import in.abhiikhula.dispute.model.DisputeState;
import in.abhiikhula.dispute.model.ResolutionAction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dispute ticket view with attached evidence (BRD FR-1302, FR-1303)")
public class DisputeResponseDto {

    private UUID id;
    private UUID shopId;
    private String shopName;
    private UUID tokenId;
    private String displayToken;
    private String customerPhone;
    private DisputeReason reason;
    private String description;
    private String evidenceJson;
    private DisputeState state;
    private String merchantReply;
    private Instant merchantReplyAt;
    private ResolutionAction resolutionAction;
    private String resolutionNotes;
    private Instant resolvedAt;
    private Instant createdAt;
}

