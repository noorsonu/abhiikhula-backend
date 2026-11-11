package in.abhiikhula.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Platform-wide operations & health metrics (BRD Section 19 / FR-801, FR-802)")
public class PlatformMetricsDto {

    @Schema(description = "Total registered non-deleted users", example = "1250")
    private long totalUsers;

    @Schema(description = "Total active customers", example = "1180")
    private long totalCustomers;

    @Schema(description = "Total shopkeeper accounts", example = "70")
    private long totalShopOwners;

    @Schema(description = "Total blocked / suspended users", example = "3")
    private long blockedUsers;

    @Schema(description = "Total registered shop listings", example = "85")
    private long totalShops;

    @Schema(description = "Verified & claimed shops", example = "52")
    private long verifiedShops;

    @Schema(description = "Unverified directory shops", example = "33")
    private long unverifiedShops;

    @Schema(description = "Active / operational shops", example = "80")
    private long activeShops;

    @Schema(description = "Suspended shops", example = "5")
    private long suspendedShops;

    @Schema(description = "Live active queues running across city right now", example = "14")
    private long activeQueuesNow;

    @Schema(description = "Total queue tokens issued platform-wide today", example = "420")
    private long tokensIssuedToday;

    @Schema(description = "Total operational pilot zones", example = "3")
    private long activeZones;

    @Schema(description = "Total supported business categories", example = "10")
    private long activeCategories;
}

