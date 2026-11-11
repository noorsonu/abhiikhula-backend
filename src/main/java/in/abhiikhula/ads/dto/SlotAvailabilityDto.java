package in.abhiikhula.ads.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Availability of featured slots for a zone and category on a given date")
public class SlotAvailabilityDto {
    private LocalDate date;
    private List<Integer> availablePositions;
    private boolean fullyBooked;
}

