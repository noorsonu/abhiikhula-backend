package in.abhiikhula.dashboard.dto;

import in.abhiikhula.shop.model.ShopStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Shop overview for merchant dashboard (BRD Section 18 / FR-701 to FR-709)")
public class MerchantShopDashboardDto {

    @Schema(description = "Shop unique ID")
    private UUID id;

    @Schema(description = "Shop name", example = "Royal Barber & Hair Style")
    private String name;

    @Schema(description = "URL slug", example = "royal-barber-indira-nagar")
    private String slug;

    @Schema(description = "Category name", example = "Salon & Barber")
    private String categoryName;

    @Schema(description = "Current operational status", example = "OPEN")
    private ShopStatus status;

    @Schema(description = "Crowd level (1-4)", example = "2")
    private Integer crowdLevel;

    @Schema(description = "Human-readable crowd label", example = "Moderate (5–15 min wait)")
    private String crowdLabel;

    @Schema(description = "Active queue session ID (null if closed)")
    private UUID activeSessionId;

    @Schema(description = "Token number currently being served (null if none)")
    private Integer servingTokenNumber;

    @Schema(description = "Total customers waiting in queue")
    private Long waitingCount;

    @Schema(description = "Max daily token cap", example = "100")
    private Integer tokenCap;

    @Schema(description = "Active delay in minutes", example = "15")
    private Integer delayMinutes;

    @Schema(description = "Reason for delay")
    private String delayReason;

    @Schema(description = "Verification status", example = "true")
    private Boolean verified;

    @Schema(description = "Contact phone number", example = "9876543210")
    private String phone;

    @Schema(description = "Shop address")
    private String address;
}

