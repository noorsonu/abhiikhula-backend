package in.abhiikhula.shop.dto;

import in.abhiikhula.shop.model.ShopStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Shop summary card shown in discovery list (BRD FR-104)")
public class ShopSummaryDto {

    @Schema(description = "Shop UUID")
    private UUID id;

    @Schema(description = "Shop display name", example = "Royal Barber & Hair Style")
    private String name;

    @Schema(description = "Shop slug for URL", example = "royal-barber-indira-nagar")
    private String slug;

    @Schema(description = "Category name", example = "Salon & Barber")
    private String categoryName;

    @Schema(description = "Category icon", example = "scissors")
    private String categoryIcon;

    @Schema(description = "Live status: OPEN, BUSY, PAUSED, CLOSED", example = "OPEN")
    private ShopStatus status;

    @Schema(description = "Crowd level 1=Empty to 4=Very Crowded", example = "2")
    private Integer crowdLevel;

    @Schema(description = "Human-readable crowd label", example = "Light (5–15 min wait)")
    private String crowdLabel;

    @Schema(description = "Latitude", example = "26.886100")
    private BigDecimal lat;

    @Schema(description = "Longitude", example = "80.995100")
    private BigDecimal lng;

    @Schema(description = "Contact number", example = "9876543210")
    private String phone;

    @Schema(description = "Whether shop is verified by admin", example = "true")
    private Boolean verified;

    @Schema(description = "Calculated distance from user in kilometers (when lat/lng provided)", example = "0.35")
    private Double distanceKm;

    @Schema(description = "Formatted distance string for UI display", example = "350 m")
    private String distanceDisplay;

    @Schema(description = "When status was last updated")
    private Instant lastStatusUpdateAt;

    @Schema(description = "Whether shop is currently a sponsored / featured listing (BRD FR-1002)", example = "true")
    @Builder.Default
    private Boolean featured = false;

    @Schema(description = "Average rating out of 5 (Null if review count < 3 per BRD FR-1105)", example = "4.8")
    private Double averageRating;

    @Schema(description = "Total verified reviews (Null if < 3)", example = "12")
    private Long reviewCount;

    @Schema(description = "Automatic ETA accuracy score (BRD FR-1107)", example = "87% accurate")
    private String etaAccuracy;
}

