package in.abhiikhula.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Aggregated shop rating summary and trust score (BRD FR-1105, FR-1107)")
public class ShopRatingSummaryDto {

    @Schema(description = "Shop UUID")
    private UUID shopId;

    @Schema(description = "Average rating out of 5 (Null if review count < 3 per BRD FR-1105)", example = "4.7")
    private Double averageRating;

    @Schema(description = "Total number of verified reviews", example = "15")
    private Long reviewCount;

    @Schema(description = "Whether average rating is displayed publicly (Requires >= 3 reviews)", example = "true")
    private Boolean isRatingVisible;

    @Schema(description = "Platform automatic ETA accuracy score (BRD FR-1107)", example = "87% accurate")
    private String etaAccuracyScore;

    @Schema(description = "List of verified customer reviews")
    private List<ReviewDto> reviews;
}

