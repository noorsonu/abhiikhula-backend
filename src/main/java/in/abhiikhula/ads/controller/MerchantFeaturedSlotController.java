package in.abhiikhula.ads.controller;

import in.abhiikhula.ads.dto.BookFeaturedSlotRequest;
import in.abhiikhula.ads.dto.FeaturedSlotDto;
import in.abhiikhula.ads.service.FeaturedSlotService;
import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shops/{shopId}/featured-slots")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "M10 Featured Listings - Merchant", description = "Shopkeeper self-serve booking of top-placement sponsored slots (BRD Section 21, FR-1001, FR-1006)")
public class MerchantFeaturedSlotController {

    private final FeaturedSlotService featuredSlotService;

    @PostMapping("/book")
    @Operation(summary = "Book featured sponsored slot", description = "Books position 1 or 2 in category discovery for a specific day (BRD FR-1001)")
    public ResponseEntity<ApiResponse<FeaturedSlotDto>> bookSlot(
            @PathVariable UUID shopId,
            @Valid @RequestBody BookFeaturedSlotRequest request,
            @AuthenticationPrincipal User currentUser) {
        request.setShopId(shopId);
        FeaturedSlotDto slot = featuredSlotService.bookFeaturedSlot(request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(slot, "Featured slot booked successfully"));
    }

    @GetMapping
    @Operation(summary = "Get shop booked featured slots", description = "Returns active and past sponsored slots with impressions and clicks for this shop")
    public ResponseEntity<ApiResponse<List<FeaturedSlotDto>>> getShopSlots(
            @PathVariable UUID shopId,
            @AuthenticationPrincipal User currentUser) {
        List<FeaturedSlotDto> slots = featuredSlotService.getShopBookedSlots(shopId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(slots, "Shop featured slots fetched successfully"));
    }
}

