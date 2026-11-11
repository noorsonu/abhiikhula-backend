package in.abhiikhula.shop.controller;

import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.shop.dto.CreateShopRequest;
import in.abhiikhula.shop.dto.ShopDetailDto;
import in.abhiikhula.shop.dto.ShopSummaryDto;
import in.abhiikhula.shop.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@Tag(name = "M1 Discovery - Shops", description = "Shop Discovery, Search & Registration APIs (BRD FR-104, FR-105, FR-106, Section 30)")
public class ShopController {

    private final ShopService shopService;

    @GetMapping
    @Operation(
        summary = "Discover shops with geolocation, filters and search",
        description = """
            Returns active shops filtered by zone, category, open-only toggle, text search query, and GPS coordinates.
            Supports distance calculation (Haversine) and sorting by STATUS, DISTANCE, or CROWD (BRD FR-104, FR-105, Section 12).
            """
    )
    public ResponseEntity<ApiResponse<List<ShopSummaryDto>>> discoverShops(
            @Parameter(description = "Zone UUID (optional if lat/lng are provided)")
            @RequestParam(required = false) UUID zoneId,
            @Parameter(description = "Category UUID (optional)")
            @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Filter only open shops (default: false to view all, true for open-only)")
            @RequestParam(required = false, defaultValue = "false") Boolean openOnly,
            @Parameter(description = "Search query for shop name, address, or category name")
            @RequestParam(required = false) String q,
            @Parameter(description = "User latitude for distance calculation (optional)", example = "26.886000")
            @RequestParam(required = false) Double lat,
            @Parameter(description = "User longitude for distance calculation (optional)", example = "80.995000")
            @RequestParam(required = false) Double lng,
            @Parameter(description = "Filter radius in kilometers (optional, requires lat/lng)", example = "5.0")
            @RequestParam(required = false) Double radiusKm,
            @Parameter(description = "Sorting criteria: STATUS, DISTANCE, CROWD (default: STATUS)", example = "DISTANCE")
            @RequestParam(required = false, defaultValue = "STATUS") String sortBy) {

        if (zoneId == null && (lat == null || lng == null)) {
            throw new in.abhiikhula.common.exception.BadRequestException("Either zoneId or both lat and lng must be provided for shop discovery");
        }

        List<ShopSummaryDto> shops = shopService.discoverShops(zoneId, categoryId, openOnly, q, lat, lng, radiusKm, sortBy);
        return ResponseEntity.ok(ApiResponse.ok(shops, "Shops fetched successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get shop detail by UUID", description = "Returns full shop detail including zone, category, operating hours and crowd info (BRD FR-110)")
    public ResponseEntity<ApiResponse<ShopDetailDto>> getShopById(@PathVariable UUID id) {
        ShopDetailDto shop = shopService.getShopById(id);
        return ResponseEntity.ok(ApiResponse.ok(shop));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get shop detail by slug", description = "Returns full shop detail via URL-friendly slug (BRD FR-110)")
    public ResponseEntity<ApiResponse<ShopDetailDto>> getShopBySlug(@PathVariable String slug) {
        ShopDetailDto shop = shopService.getShopBySlug(slug);
        return ResponseEntity.ok(ApiResponse.ok(shop));
    }

    @PostMapping
    @Operation(summary = "Register / Onboard a new shop", description = "Onboard a new shop into a zone and category (BRD M6 / Section 17)")
    public ResponseEntity<ApiResponse<ShopDetailDto>> createShop(@Valid @RequestBody CreateShopRequest request) {
        ShopDetailDto createdShop = shopService.createShop(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(createdShop, "Shop registered successfully"));
    }
}
