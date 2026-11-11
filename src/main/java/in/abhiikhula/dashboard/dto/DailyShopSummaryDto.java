package in.abhiikhula.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Daily aggregate analytics for shopkeeper (BRD Section 18.2 / FR-710)")
public class DailyShopSummaryDto {

    @Schema(description = "Shop unique ID")
    private UUID shopId;

    @Schema(description = "Shop name", example = "Royal Barber & Hair Style")
    private String shopName;

    @Schema(description = "Summary date", example = "2026-11-10")
    private LocalDate date;

    @Schema(description = "Total tokens issued today", example = "45")
    private int totalTokensIssued;

    @Schema(description = "Total tokens completed today", example = "38")
    private int totalTokensCompleted;

    @Schema(description = "Total no-show tokens", example = "4")
    private int totalNoShows;

    @Schema(description = "Total cancelled tokens", example = "2")
    private int totalCancelled;

    @Schema(description = "Total skipped tokens", example = "1")
    private int totalSkipped;

    @Schema(description = "Average wait time in minutes before being called", example = "18.5")
    private double averageWaitTimeMinutes;

    @Schema(description = "Average service duration in minutes", example = "22.0")
    private double averageServiceTimeMinutes;

    @Schema(description = "Peak traffic hour (e.g. 17:00 - 18:00)", example = "17:00 - 18:00")
    private String peakHour;

    @Schema(description = "Total operational hours today", example = "8.5")
    private double totalOperationalHours;
}

