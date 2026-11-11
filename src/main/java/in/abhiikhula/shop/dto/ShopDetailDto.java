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
@Schema(description = "Full shop detail view (BRD FR-105)")
public class ShopDetailDto {

    @Schema(description = "Shop UUID")
    private UUID id;

    @Schema(description = "Zone UUID")
    private UUID zoneId;

    @Schema(description = "Zone name", example = "Indira Nagar (Sector 14)")
    private String zoneName;

    @Schema(description = "Category UUID")
    private UUID categoryId;

    @Schema(description = "Category name", example = "Salon & Barber")
    private String categoryName;

    @Schema(description = "Category icon", example = "scissors")
    private String categoryIcon;

    @Schema(description = "Default service time in minutes (for ETA cold-start)", example = "15")
    private Integer defaultServiceMin;

    @Schema(description = "Shop name", example = "Royal Barber & Hair Style")
    private String name;

    @Schema(description = "Shop slug", example = "royal-barber-indira-nagar")
    private String slug;

    @Schema(description = "Full address")
    private String address;

    @Schema(description = "Latitude", example = "26.886100")
    private BigDecimal lat;

    @Schema(description = "Longitude", example = "80.995100")
    private BigDecimal lng;

    @Schema(description = "Contact phone", example = "9876543210")
    private String phone;

    @Schema(description = "Live status: OPEN / BUSY / PAUSED / CLOSED", example = "OPEN")
    private ShopStatus status;

    @Schema(description = "Crowd level 1–4", example = "2")
    private Integer crowdLevel;

    @Schema(description = "Human-readable crowd label", example = "Light (5–15 min wait)")
    private String crowdLabel;

    @Schema(description = "Max tokens allowed per queue session", example = "50")
    private Integer tokenCap;

    @Schema(description = "Admin verified flag", example = "true")
    private Boolean verified;

    @Schema(description = "Governance strikes count (BRD Section 22, 24)", example = "0")
    private Integer strikes;

    @Schema(description = "Weekly operating hours")
    private java.util.List<ShopHoursDto> operatingHours;

    @Schema(description = "When status was last updated")
    private Instant lastStatusUpdateAt;

    @Schema(description = "Average rating out of 5 (Null if review count < 3 per BRD FR-1105)", example = "4.8")
    private Double averageRating;

    @Schema(description = "Total verified reviews (Null if < 3)", example = "12")
    private Long reviewCount;

    @Schema(description = "Automatic ETA accuracy score (BRD FR-1107)", example = "87% accurate")
    private String etaAccuracy;

    @Schema(description = "Shop registration timestamp")
    private Instant createdAt;
}

