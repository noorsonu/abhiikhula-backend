package in.abhiikhula.ads.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Featured sponsored slot representation")
public class FeaturedSlotDto {
    private UUID id;
    private UUID zoneId;
    private String zoneName;
    private UUID categoryId;
    private String categoryName;
    private UUID shopId;
    private String shopName;
    private LocalDate slotDate;
    private Integer position;
    private Integer impressions;
    private Integer clicks;
    private Boolean active;
    private Instant createdAt;
}

