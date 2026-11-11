package in.abhiikhula.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Merchant rolling 7-day or 30-day performance report (BRD Section 23.1, FR-1202)")
public class ShopPeriodReportDto {

    private UUID shopId;
    private String shopName;
    private Integer periodDays;
    private Long totalTokens;
    private Long totalServed;
    private Long totalNoShows;
    private Double noShowRatePct;
    private Double averageWaitMin;
    private String peakHour;
    private Long estimatedLostWalkIns;
    private List<PeriodMetricPointDto> trendPoints;
}

