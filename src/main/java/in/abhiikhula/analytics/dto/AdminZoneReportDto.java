package in.abhiikhula.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Admin aggregate health and activity report for a zone (BRD Section 23.2)")
public class AdminZoneReportDto {

    private UUID zoneId;
    private String zoneName;
    private Long totalShops;
    private Long liveOpenShops;
    private Long totalTokensIssued;
    private Long totalTokensServed;
    private Double overallCompliancePct;
    private Long activeSubscribers;
}

