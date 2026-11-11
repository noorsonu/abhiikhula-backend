package in.abhiikhula.shop.dto;

import in.abhiikhula.shop.model.ShopStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Single audit record of a shop status transition")
public class StatusEventHistoryDto {

    private UUID id;
    private ShopStatus fromStatus;
    private ShopStatus toStatus;
    private Integer crowdLevel;
    private String crowdLabel;
    private String source;
    private String note;
    private Instant createdAt;
}

