package in.abhiikhula.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Business Category / Vertical Details (BRD Section 29)")
public class CategoryResponseDto {

    @Schema(description = "Category UUID")
    private UUID id;

    @Schema(description = "Category display name", example = "Salon & Barber")
    private String name;

    @Schema(description = "Unique category slug for URLs and queries", example = "salon-barber")
    private String slug;

    @Schema(description = "Icon identifier", example = "scissors")
    private String icon;

    @Schema(description = "Default service duration in minutes used for cold-start ETA calculation", example = "15")
    private Integer defaultServiceMin;

    @Schema(description = "Category active status", example = "true")
    private Boolean active;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;
}

