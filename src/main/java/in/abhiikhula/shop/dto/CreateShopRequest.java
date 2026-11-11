package in.abhiikhula.shop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload to register a new shop (BRD M6 / Section 17)")
public class CreateShopRequest {

    @NotNull(message = "Zone ID is mandatory")
    @Schema(description = "UUID of the zone where shop is located")
    private UUID zoneId;

    @NotNull(message = "Category ID is mandatory")
    @Schema(description = "UUID of the shop vertical/category")
    private UUID categoryId;

    @NotBlank(message = "Shop name is mandatory")
    @Schema(description = "Shop display name", example = "Royal Barber & Hair Style")
    private String name;

    @NotBlank(message = "Address is mandatory")
    @Schema(description = "Full street address", example = "Shop No. 12, Sector 14 Market, Indira Nagar, Lucknow")
    private String address;

    @NotNull(message = "Latitude is mandatory")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    @Schema(description = "Latitude coordinate", example = "26.886100")
    private BigDecimal lat;

    @NotNull(message = "Longitude is mandatory")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    @Schema(description = "Longitude coordinate", example = "80.995100")
    private BigDecimal lng;

    @Schema(description = "Contact phone number", example = "9876543210")
    private String phone;

    @Schema(description = "Max daily token cap (default 100)", example = "100")
    @Builder.Default
    private Integer tokenCap = 100;
}

