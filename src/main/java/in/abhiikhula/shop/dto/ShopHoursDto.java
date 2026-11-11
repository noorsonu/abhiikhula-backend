package in.abhiikhula.shop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Operating hours for a specific weekday (1=Monday, 7=Sunday)")
public class ShopHoursDto {

    @Schema(description = "Weekday number (1=Mon, 2=Tue, ..., 7=Sun)", example = "1")
    private Integer weekday;

    @Schema(description = "Opening time", example = "09:00:00")
    private LocalTime openTime;

    @Schema(description = "Closing time", example = "21:30:00")
    private LocalTime closeTime;

    @Schema(description = "Whether shop remains closed all day", example = "false")
    private Boolean closedAllDay;
}

