package in.abhiikhula.review.dto;

import in.abhiikhula.review.model.ReviewTag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to submit a post-service rating (BRD FR-1101, FR-1102)")
public class CreateReviewRequest {

    @NotNull(message = "Token ID is required to verify actual service")
    @Schema(description = "Completed/Served token UUID", example = "d1b2c3d4-0000-0000-0000-000000000001")
    private UUID tokenId;

    @NotNull(message = "Rating stars are required")
    @Min(value = 1, message = "Rating must be at least 1 star")
    @Max(value = 5, message = "Rating cannot exceed 5 stars")
    @Schema(description = "Rating stars from 1 to 5", example = "5")
    private Integer stars;

    @Schema(description = "Optional detailed review text", example = "Very quick cut, followed the queue properly.")
    private String text;

    @Schema(description = "Quick feedback chips (BRD FR-1102: ON_TIME, BEHAVIOUR, FAIR_WAIT)")
    private List<ReviewTag> tags;

    @Schema(description = "Optional guest reviewer name", example = "Pooja S.")
    private String customerName;
}

