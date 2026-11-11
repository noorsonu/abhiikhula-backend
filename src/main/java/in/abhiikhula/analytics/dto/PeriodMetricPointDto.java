package in.abhiikhula.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Single date trend point for merchant charts")
public class PeriodMetricPointDto {
    private LocalDate date;
    private Long tokensIssued;
    private Long tokensServed;
    private Long noShows;
    private Double averageWaitMin;
}

