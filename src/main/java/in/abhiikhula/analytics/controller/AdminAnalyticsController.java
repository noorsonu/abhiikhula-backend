package in.abhiikhula.analytics.controller;

import in.abhiikhula.analytics.dto.AdminFunnelReportDto;
import in.abhiikhula.analytics.dto.AdminZoneReportDto;
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
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "M12 Analytics - Admin Reporting", description = "Platform-wide conversion funnel, zone health, and CSV streaming (BRD Section 23.2, FR-808, FR-1203)")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/zones/{zoneId}")
    @Operation(summary = "Admin: Get zone health report", description = "Returns active shops, compliance, token counts, and subscriber breakdown for zone")
    public ResponseEntity<ApiResponse<AdminZoneReportDto>> getZoneReport(
            @PathVariable UUID zoneId,
            @AuthenticationPrincipal User admin) {
        AdminZoneReportDto report = analyticsService.getZoneReport(zoneId, admin);
        return ResponseEntity.ok(ApiResponse.ok(report, "Zone analytics report fetched successfully"));
    }

    @GetMapping("/funnel")
    @Operation(summary = "Admin: Get platform conversion funnel", description = "Computes search -> detail view -> token issue -> served conversion percentages (FR-1201)")
    public ResponseEntity<ApiResponse<AdminFunnelReportDto>> getFunnelReport(
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User admin) {

        Instant start = startDate != null ? startDate.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant end = endDate != null ? endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;

        AdminFunnelReportDto funnel = analyticsService.getFunnelReport(zoneId, start, end, admin);
        return ResponseEntity.ok(ApiResponse.ok(funnel, "Funnel report fetched successfully"));
    }

    @GetMapping("/export/tokens.csv")
    @Operation(summary = "Admin: Export platform tokens to CSV", description = "Streams up to 50,000 platform queue records without customer PII (FR-808, FR-1203)")
    public void exportTokensCsv(
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) UUID shopId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User admin,
            HttpServletResponse response) throws IOException {

        Instant start = startDate != null ? startDate.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant end = endDate != null ? endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;

        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"platform_tokens.csv\"");

        analyticsService.exportTokensCsv(shopId, zoneId, start, end, admin, response.getWriter());
    }

    @GetMapping("/export/shops.csv")
    @Operation(summary = "Admin: Export platform shops to CSV", description = "Streams platform registered shops list to CSV (FR-808)")
    public void exportShopsCsv(
            @RequestParam(required = false) UUID zoneId,
            @AuthenticationPrincipal User admin,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"platform_shops.csv\"");

        analyticsService.exportShopsCsv(zoneId, admin, response.getWriter());
    }
}

