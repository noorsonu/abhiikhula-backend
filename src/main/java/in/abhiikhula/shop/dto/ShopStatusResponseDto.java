package in.abhiikhula.shop.dto;

import in.abhiikhula.shop.model.ShopStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Live shop status, crowd level, and recent transition history")
public class ShopStatusResponseDto {

    private UUID shopId;
    private String shopName;
    private ShopStatus status;
    private Integer crowdLevel;
    private String crowdLabel;
    private String note;
    private Instant lastStatusUpdateAt;
    private List<StatusEventHistoryDto> recentEvents;
}

