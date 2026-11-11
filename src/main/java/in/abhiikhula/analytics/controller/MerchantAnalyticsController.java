package in.abhiikhula.analytics.controller;

import in.abhiikhula.analytics.dto.ShopPeriodReportDto;
import in.abhiikhula.analytics.service.AnalyticsService;
import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/shops/{shopId}/analytics")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "M12 Analytics - Merchant Reporting", description = "Shopkeeper rolling 7 and 30-day performance trends and CSV export (BRD Section 23.1, FR-1202, FR-1203)")
public class MerchantAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/report")
    @Operation(summary = "Get merchant performance report", description = "Returns rolling 7 or 30 days metrics: tokens, wait time, no-shows, peak hours, trend line (FR-1202)")
    public ResponseEntity<ApiResponse<ShopPeriodReportDto>> getPeriodReport(
            @PathVariable UUID shopId,
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal User merchant) {
        ShopPeriodReportDto report = analyticsService.getShopPeriodReport(shopId, days, merchant);
        return ResponseEntity.ok(ApiResponse.ok(report, "Shop period report fetched successfully"));
    }

    @GetMapping("/export/tokens.csv")
    @Operation(summary = "Export shop tokens to CSV", description = "Streams up to 50,000 shop queue records to CSV without customer PII (FR-1203, FR-808)")
    public void exportTokensCsv(
            @PathVariable UUID shopId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User merchant,
            HttpServletResponse response) throws IOException {

        Instant start = startDate != null ? startDate.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant end = endDate != null ? endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;

        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"shop_" + shopId + "_tokens.csv\"");

        analyticsService.exportTokensCsv(shopId, null, start, end, merchant, response.getWriter());
    }
}

