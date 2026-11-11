package in.abhiikhula.ads.controller;

import in.abhiikhula.ads.dto.SlotAvailabilityDto;
import in.abhiikhula.ads.service.FeaturedSlotService;
import in.abhiikhula.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/ads/featured")
@RequiredArgsConstructor
@Tag(name = "M10 Featured Listings - Public", description = "Public availability check and engagement tracking for sponsored slots (BRD Section 21)")
public class FeaturedSlotController {

    private final FeaturedSlotService featuredSlotService;

    @GetMapping("/availability")
    @Operation(summary = "Check featured slot availability", description = "Returns available positions (max 2) for a given zone and category on a specific date (BRD FR-1001)")
    public ResponseEntity<ApiResponse<SlotAvailabilityDto>> getAvailability(
            @RequestParam UUID zoneId,
            @RequestParam UUID categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        SlotAvailabilityDto availability = featuredSlotService.getSlotAvailability(zoneId, categoryId, date);
        return ResponseEntity.ok(ApiResponse.ok(availability, "Featured slot availability fetched successfully"));
    }

    @PostMapping("/{slotId}/click")
    @Operation(summary = "Record sponsored ad click", description = "Increments click counter for performance analytics (BRD FR-1003)")
    public ResponseEntity<ApiResponse<Void>> recordClick(@PathVariable UUID slotId) {
        featuredSlotService.recordClick(slotId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Click recorded"));
    }
}

