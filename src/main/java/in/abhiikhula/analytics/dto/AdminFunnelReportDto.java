package in.abhiikhula.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Platform-wide discovery-to-service funnel metrics (BRD Section 23.2)")
public class AdminFunnelReportDto {

    private Long searchViews;
    private Long detailViews;
    private Long tokensTaken;
    private Long checkins;
    private Long served;
    private Double viewToTokenConversionPct;
    private Double tokenToServeConversionPct;
}

