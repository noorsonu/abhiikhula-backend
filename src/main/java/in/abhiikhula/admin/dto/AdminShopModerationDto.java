package in.abhiikhula.admin.dto;

import in.abhiikhula.shop.model.ShopStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Shop moderation details for admin panel (BRD FR-804, FR-805)")
public class AdminShopModerationDto {

    @Schema(description = "Shop UUID")
    private UUID shopId;

    @Schema(description = "Shop name", example = "Royal Barber & Hair Style")
    private String name;

    @Schema(description = "Shop operational status")
    private ShopStatus status;

    @Schema(description = "Whether shop is active on platform")
    private Boolean active;

    @Schema(description = "Whether shop is claimed and verified")
    private Boolean verified;

    @Schema(description = "Owner user ID")
    private UUID ownerId;

    @Schema(description = "Owner phone number")
    private String ownerPhone;

    @Schema(description = "Summary message of moderation action")
    private String message;
}

