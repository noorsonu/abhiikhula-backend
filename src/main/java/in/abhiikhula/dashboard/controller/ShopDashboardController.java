package in.abhiikhula.dashboard.controller;

import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.common.exception.UnauthorizedException;
import in.abhiikhula.dashboard.dto.DailyShopSummaryDto;
import in.abhiikhula.dashboard.dto.MerchantShopDashboardDto;
import in.abhiikhula.dashboard.service.ShopDashboardService;
import in.abhiikhula.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "M7 Shopkeeper Dashboard & Analytics", description = "Merchant Shop Control Panel & End-of-Day Analytics (BRD Section 18, FR-701 to FR-710)")
public class ShopDashboardController {

    private final ShopDashboardService shopDashboardService;

    @GetMapping("/my")
    @Operation(summary = "Get merchant owned shops with live queue stats", description = "Returns all shops owned by currently logged-in shopkeeper with active token numbers and waiting counts (BRD Section 18.1)")
    public ResponseEntity<ApiResponse<List<MerchantShopDashboardDto>>> getMyShops(
            @AuthenticationPrincipal User merchant) {

        if (merchant == null) {
            throw new UnauthorizedException("Authentication required to view merchant shops");
        }

        List<MerchantShopDashboardDto> shops = shopDashboardService.getMyShops(merchant);
        return ResponseEntity.ok(ApiResponse.ok(shops, "Merchant shops fetched successfully"));
    }

    @GetMapping("/{shopId}/analytics/daily")
    @Operation(summary = "Get End-of-Day daily analytics summary", description = "Returns daily aggregates: tokens issued, served, no-shows, average wait time, and peak hour (BRD Section 18.2 / FR-710)")
    public ResponseEntity<ApiResponse<DailyShopSummaryDto>> getDailySummary(
            @PathVariable UUID shopId,
            @Parameter(description = "Date for summary in YYYY-MM-DD format (defaults to today UTC)", example = "2026-11-10")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal User merchant) {

        if (merchant == null) {
            throw new UnauthorizedException("Authentication required to view shop analytics");
        }

        DailyShopSummaryDto summary = shopDashboardService.getDailySummary(shopId, date, merchant);
        return ResponseEntity.ok(ApiResponse.ok(summary, "Daily shop analytics fetched successfully"));
    }
}

