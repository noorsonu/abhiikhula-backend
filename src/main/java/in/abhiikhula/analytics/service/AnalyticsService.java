package in.abhiikhula.analytics.service;

import in.abhiikhula.analytics.dto.AdminFunnelReportDto;
import in.abhiikhula.analytics.dto.AdminZoneReportDto;
import in.abhiikhula.analytics.dto.RecordEventRequest;
import in.abhiikhula.analytics.dto.ShopPeriodReportDto;
import in.abhiikhula.user.model.User;

import java.io.PrintWriter;
import java.time.Instant;
import java.util.UUID;

public interface AnalyticsService {

    void recordEvent(RecordEventRequest request, User currentUser);

    ShopPeriodReportDto getShopPeriodReport(UUID shopId, int days, User merchant);

    AdminFunnelReportDto getFunnelReport(UUID zoneId, Instant start, Instant end, User admin);

    AdminZoneReportDto getZoneReport(UUID zoneId, User admin);

    void exportTokensCsv(UUID shopId, UUID zoneId, Instant start, Instant end, User user, PrintWriter writer);

    void exportShopsCsv(UUID zoneId, User admin, PrintWriter writer);
}

