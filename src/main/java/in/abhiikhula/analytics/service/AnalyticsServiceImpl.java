package in.abhiikhula.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.analytics.dto.*;
import in.abhiikhula.analytics.model.AnalyticsEvent;
import in.abhiikhula.analytics.repository.AnalyticsEventRepository;
import in.abhiikhula.common.exception.BadRequestException;
import in.abhiikhula.common.exception.ForbiddenException;
import in.abhiikhula.common.exception.ResourceNotFoundException;
import in.abhiikhula.queue.model.Token;
import in.abhiikhula.queue.model.TokenStatus;
import in.abhiikhula.queue.repository.TokenRepository;
import in.abhiikhula.shop.model.Shop;
import in.abhiikhula.shop.model.ShopStatus;
import in.abhiikhula.shop.repository.ShopRepository;
import in.abhiikhula.user.model.User;
import in.abhiikhula.user.model.UserRole;
import in.abhiikhula.zone.model.Zone;
import in.abhiikhula.zone.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsEventRepository analyticsEventRepository;
    private final TokenRepository tokenRepository;
    private final ShopRepository shopRepository;
    private final ZoneRepository zoneRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void recordEvent(RecordEventRequest request, User currentUser) {
        Shop shop = null;
        if (request.getShopId() != null) {
            shop = shopRepository.findById(request.getShopId()).orElse(null);
        }

        Zone zone = null;
        if (request.getZoneId() != null) {
            zone = zoneRepository.findById(request.getZoneId()).orElse(null);
        } else if (shop != null && shop.getZone() != null) {
            zone = shop.getZone();
        }

        // FR-1204: Anonymize and strip potential PII from props
        String propsJson = null;
        if (request.getProps() != null && !request.getProps().isEmpty()) {
            Map<String, Object> sanitized = new HashMap<>(request.getProps());
            sanitized.remove("phone");
            sanitized.remove("email");
            sanitized.remove("password");
            sanitized.remove("customerPhone");
            sanitized.remove("customerEmail");
            try {
                propsJson = objectMapper.writeValueAsString(sanitized);
            } catch (Exception e) {
                log.warn("Failed to serialize analytics props: {}", e.getMessage());
            }
        }

        AnalyticsEvent event = AnalyticsEvent.builder()
                .name(request.getName().trim().toUpperCase())
                .deviceId(request.getDeviceId() != null ? request.getDeviceId().trim() : null)
                .user(currentUser)
                .shop(shop)
                .zone(zone)
                .props(propsJson)
                .build();

        analyticsEventRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopPeriodReportDto getShopPeriodReport(UUID shopId, int days, User merchant) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));

        verifyShopOwner(shop, merchant);

        int period = (days == 30) ? 30 : 7;
        Instant start = Instant.now().minus(Duration.ofDays(period));
        Instant end = Instant.now();

        List<Token> tokens = tokenRepository.findByShopIdAndJoinedAtBetween(shopId, start, end);

        long totalTokens = tokens.size();
        long totalServed = tokens.stream().filter(t -> t.getStatus() == TokenStatus.COMPLETED).count();
        long totalNoShows = tokens.stream().filter(t -> t.getStatus() == TokenStatus.NO_SHOW).count();
        long totalCancelled = tokens.stream().filter(t -> t.getStatus() == TokenStatus.CANCELLED).count();

        double noShowRate = totalTokens > 0 ? (totalNoShows * 100.0 / totalTokens) : 0.0;
        noShowRate = Math.round(noShowRate * 10.0) / 10.0;

        List<Token> completedTokens = tokens.stream()
                .filter(t -> t.getStatus() == TokenStatus.COMPLETED && t.getJoinedAt() != null && t.getServedAt() != null)
                .toList();

        double avgWait = 0.0;
        if (!completedTokens.isEmpty()) {
            long totalWaitMin = completedTokens.stream()
                    .mapToLong(t -> Duration.between(t.getJoinedAt(), t.getServedAt()).toMinutes())
                    .sum();
            avgWait = (double) totalWaitMin / completedTokens.size();
            avgWait = Math.round(avgWait * 10.0) / 10.0;
        }

        // Peak hour calculation
        String peakHour = "11:00 AM - 12:00 PM";
        if (!tokens.isEmpty()) {
            Map<Integer, Long> hourCounts = tokens.stream()
                    .map(t -> LocalDateTime.ofInstant(t.getJoinedAt(), ZoneId.systemDefault()).getHour())
                    .collect(Collectors.groupingBy(h -> h, Collectors.counting()));

            int peak = hourCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(11);

            peakHour = String.format("%02d:00 - %02d:00", peak, (peak + 1) % 24);
        }

        // Trend points per day
        Map<LocalDate, List<Token>> tokensByDate = tokens.stream()
                .collect(Collectors.groupingBy(t -> LocalDate.ofInstant(t.getJoinedAt(), ZoneId.systemDefault())));

        List<PeriodMetricPointDto> trendPoints = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = period - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            List<Token> dayTokens = tokensByDate.getOrDefault(d, Collections.emptyList());
            long dayIssued = dayTokens.size();
            long dayServed = dayTokens.stream().filter(t -> t.getStatus() == TokenStatus.COMPLETED).count();
            long dayNoShows = dayTokens.stream().filter(t -> t.getStatus() == TokenStatus.NO_SHOW).count();

            double dayAvgWait = 0.0;
            List<Token> dayCompleted = dayTokens.stream()
                    .filter(t -> t.getStatus() == TokenStatus.COMPLETED && t.getJoinedAt() != null && t.getServedAt() != null)
                    .toList();
            if (!dayCompleted.isEmpty()) {
                long sum = dayCompleted.stream().mapToLong(t -> Duration.between(t.getJoinedAt(), t.getServedAt()).toMinutes()).sum();
                dayAvgWait = Math.round(((double) sum / dayCompleted.size()) * 10.0) / 10.0;
            }

            trendPoints.add(PeriodMetricPointDto.builder()
                    .date(d)
                    .tokensIssued(dayIssued)
                    .tokensServed(dayServed)
                    .noShows(dayNoShows)
                    .averageWaitMin(dayAvgWait)
                    .build());
        }

        return ShopPeriodReportDto.builder()
                .shopId(shop.getId())
                .shopName(shop.getName())
                .periodDays(period)
                .totalTokens(totalTokens)
                .totalServed(totalServed)
                .totalNoShows(totalNoShows)
                .noShowRatePct(noShowRate)
                .averageWaitMin(avgWait)
                .peakHour(peakHour)
                .estimatedLostWalkIns(totalNoShows + totalCancelled)
                .trendPoints(trendPoints)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminFunnelReportDto getFunnelReport(UUID zoneId, Instant start, Instant end, User admin) {
        validateAdmin(admin);

        Instant s = start != null ? start : Instant.now().minus(Duration.ofDays(30));
        Instant e = end != null ? end : Instant.now();

        long searchViews = zoneId != null ?
                analyticsEventRepository.countByZoneIdAndNameAndCreatedAtBetween(zoneId, "SEARCH_OPEN", s, e) :
                analyticsEventRepository.countByNameAndCreatedAtBetween("SEARCH_OPEN", s, e);

        long detailViews = zoneId != null ?
                analyticsEventRepository.countByZoneIdAndNameAndCreatedAtBetween(zoneId, "DETAIL_OPEN", s, e) :
                analyticsEventRepository.countByNameAndCreatedAtBetween("DETAIL_OPEN", s, e);

        long tokensTaken = zoneId != null ?
                analyticsEventRepository.countByZoneIdAndNameAndCreatedAtBetween(zoneId, "TOKEN_ISSUED", s, e) :
                analyticsEventRepository.countByNameAndCreatedAtBetween("TOKEN_ISSUED", s, e);

        long checkins = zoneId != null ?
                analyticsEventRepository.countByZoneIdAndNameAndCreatedAtBetween(zoneId, "CHECKIN", s, e) :
                analyticsEventRepository.countByNameAndCreatedAtBetween("CHECKIN", s, e);

        long served = zoneId != null ?
                analyticsEventRepository.countByZoneIdAndNameAndCreatedAtBetween(zoneId, "SERVED", s, e) :
                analyticsEventRepository.countByNameAndCreatedAtBetween("SERVED", s, e);

        double viewToTokenPct = detailViews > 0 ? (tokensTaken * 100.0 / detailViews) : 0.0;
        double tokenToServePct = tokensTaken > 0 ? (served * 100.0 / tokensTaken) : 0.0;

        return AdminFunnelReportDto.builder()
                .searchViews(searchViews)
                .detailViews(detailViews)
                .tokensTaken(tokensTaken)
                .checkins(checkins)
                .served(served)
                .viewToTokenConversionPct(Math.round(viewToTokenPct * 10.0) / 10.0)
                .tokenToServeConversionPct(Math.round(tokenToServePct * 10.0) / 10.0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminZoneReportDto getZoneReport(UUID zoneId, User admin) {
        validateAdmin(admin);

        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", zoneId));

        List<Shop> shops = shopRepository.findByZoneId(zoneId);
        long totalShops = shops.size();
        long openShops = shops.stream().filter(s -> s.getStatus() == ShopStatus.OPEN || s.getStatus() == ShopStatus.BUSY).count();

        Instant start = Instant.now().minus(Duration.ofDays(7));
        Instant end = Instant.now();

        long tokensIssued = shops.stream()
                .mapToLong(s -> tokenRepository.findByShopIdAndJoinedAtBetween(s.getId(), start, end).size())
                .sum();

        long tokensServed = shops.stream()
                .mapToLong(s -> tokenRepository.findByShopIdAndJoinedAtBetween(s.getId(), start, end).stream()
                        .filter(t -> t.getStatus() == TokenStatus.COMPLETED).count())
                .sum();

        return AdminZoneReportDto.builder()
                .zoneId(zone.getId())
                .zoneName(zone.getName())
                .totalShops(totalShops)
                .liveOpenShops(openShops)
                .totalTokensIssued(tokensIssued)
                .totalTokensServed(tokensServed)
                .overallCompliancePct(85.0)
                .activeSubscribers(totalShops) // Pilot all shops seeded with Free Pro
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void exportTokensCsv(UUID shopId, UUID zoneId, Instant start, Instant end, User user, PrintWriter writer) {
        validateCanExport(shopId, user);

        Instant s = start != null ? start : Instant.now().minus(Duration.ofDays(30));
        Instant e = end != null ? end : Instant.now();

        List<Token> tokens;
        if (shopId != null) {
            tokens = tokenRepository.findByShopIdAndJoinedAtBetween(shopId, s, e);
        } else {
            tokens = tokenRepository.findAll().stream()
                    .filter(t -> t.getJoinedAt() != null && t.getJoinedAt().isAfter(s) && t.getJoinedAt().isBefore(e))
                    .limit(50000)
                    .toList();
        }

        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;

        // CSV Header (FR-1204 / FR-1603: No raw customer phone)
        writer.println("Token ID,Display Token,Shop Name,Status,Estimated Wait Min,Joined At,Called At,Served At,Checked In");

        for (Token t : tokens) {
            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\",\"%s\",%b%n",
                    t.getId(),
                    t.getDisplayToken() != null ? t.getDisplayToken() : "",
                    t.getShop() != null ? t.getShop().getName().replace("\"", "\"\"") : "",
                    t.getStatus(),
                    t.getEstimatedWaitMin() != null ? t.getEstimatedWaitMin() : 0,
                    t.getJoinedAt() != null ? dtf.format(t.getJoinedAt()) : "",
                    t.getCalledAt() != null ? dtf.format(t.getCalledAt()) : "",
                    t.getServedAt() != null ? dtf.format(t.getServedAt()) : "",
                    Boolean.TRUE.equals(t.getCheckedIn())
            );
        }
        writer.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public void exportShopsCsv(UUID zoneId, User admin, PrintWriter writer) {
        validateAdmin(admin);

        List<Shop> shops = zoneId != null ? shopRepository.findByZoneId(zoneId) : shopRepository.findAll();

        writer.println("Shop ID,Name,Slug,Zone,Category,Status,Crowd Level,Verified,Token Cap,Created At");
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;

        for (Shop s : shops) {
            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,%b,%d,\"%s\"%n",
                    s.getId(),
                    s.getName().replace("\"", "\"\""),
                    s.getSlug(),
                    s.getZone() != null ? s.getZone().getName() : "",
                    s.getCategory() != null ? s.getCategory().getName() : "",
                    s.getStatus(),
                    s.getCrowdLevel(),
                    Boolean.TRUE.equals(s.getVerified()),
                    s.getTokenCap(),
                    s.getCreatedAt() != null ? dtf.format(s.getCreatedAt()) : ""
            );
        }
        writer.flush();
    }

    private void verifyShopOwner(Shop shop, User user) {
        if (user == null) {
            throw new ForbiddenException("Authentication required");
        }
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (shop.getOwner() == null || !shop.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("You are not authorized to view reports for this shop");
        }
    }

    private void validateCanExport(UUID shopId, User user) {
        if (user == null) {
            throw new ForbiddenException("Authentication required to export CSV reports");
        }
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (shopId != null) {
            Shop shop = shopRepository.findById(shopId)
                    .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));
            if (shop.getOwner() != null && shop.getOwner().getId().equals(user.getId())) {
                return;
            }
        }
        throw new ForbiddenException("Admin or shop owner privileges required to export CSV");
    }

    private void validateAdmin(User admin) {
        if (admin == null || admin.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Admin privileges required to access this resource");
        }
    }
}

