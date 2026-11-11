package in.abhiikhula.ads.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to book a featured slot for a shop (BRD FR-1001)")
public class BookFeaturedSlotRequest {

    @NotNull(message = "Shop ID is required")
    @Schema(description = "Shop UUID to boost")
    private UUID shopId;

    @NotNull(message = "Slot date is required")
    @FutureOrPresent(message = "Slot date must be today or in the future")
    @Schema(description = "Date for sponsored listing", example = "2026-11-12")
    private LocalDate slotDate;

    @Min(value = 1, message = "Position must be 1 or 2")
    @Max(value = 2, message = "Position must be 1 or 2")
    @Schema(description = "Preferred position (1 or 2). If omitted, next available position is allocated.", example = "1")
    private Integer preferredPosition;
}

