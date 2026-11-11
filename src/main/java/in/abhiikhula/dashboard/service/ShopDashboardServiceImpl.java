package in.abhiikhula.dashboard.service;

import in.abhiikhula.common.exception.ForbiddenException;
import in.abhiikhula.common.exception.ResourceNotFoundException;
import in.abhiikhula.dashboard.dto.DailyShopSummaryDto;
import in.abhiikhula.dashboard.dto.MerchantShopDashboardDto;
import in.abhiikhula.queue.model.QueueSession;
import in.abhiikhula.queue.model.SessionStatus;
import in.abhiikhula.queue.model.Token;
import in.abhiikhula.queue.model.TokenStatus;
import in.abhiikhula.queue.repository.QueueSessionRepository;
import in.abhiikhula.queue.repository.TokenRepository;
import in.abhiikhula.shop.model.Shop;
import in.abhiikhula.shop.model.ShopStatus;
import in.abhiikhula.shop.model.ShopStatusEvent;
import in.abhiikhula.shop.repository.ShopRepository;
import in.abhiikhula.shop.repository.ShopStatusEventRepository;
import in.abhiikhula.user.model.User;
import in.abhiikhula.user.model.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopDashboardServiceImpl implements ShopDashboardService {

    private final ShopRepository shopRepository;
    private final QueueSessionRepository queueSessionRepository;
    private final TokenRepository tokenRepository;
    private final ShopStatusEventRepository shopStatusEventRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MerchantShopDashboardDto> getMyShops(User merchant) {
        log.info("Fetching merchant dashboard shops for user: {}", merchant.getId());
        List<Shop> shops = shopRepository.findByOwnerId(merchant.getId());

        List<MerchantShopDashboardDto> result = new ArrayList<>();
        for (Shop shop : shops) {
            Optional<QueueSession> activeSessionOpt = queueSessionRepository
                    .findByShopIdAndStatusIn(shop.getId(), List.of(SessionStatus.ACTIVE, SessionStatus.PAUSED));

            UUID activeSessionId = null;
            Integer servingTokenNumber = null;
            Long waitingCount = 0L;
            Integer delayMinutes = null;
            String delayReason = null;

            if (activeSessionOpt.isPresent()) {
                QueueSession session = activeSessionOpt.get();
                activeSessionId = session.getId();
                servingTokenNumber = session.getCurrentTokenNumber();
                waitingCount = tokenRepository.countBySessionIdAndStatus(session.getId(), TokenStatus.WAITING);
                delayMinutes = session.getDelayMinutes();
                delayReason = session.getDelayReason();
            }

            result.add(MerchantShopDashboardDto.builder()
                    .id(shop.getId())
                    .name(shop.getName())
                    .slug(shop.getSlug())
                    .categoryName(shop.getCategory() != null ? shop.getCategory().getName() : null)
                    .status(shop.getStatus())
                    .crowdLevel(shop.getCrowdLevel())
                    .crowdLabel(getCrowdLabel(shop.getCrowdLevel()))
                    .activeSessionId(activeSessionId)
                    .servingTokenNumber(servingTokenNumber)
                    .waitingCount(waitingCount)
                    .tokenCap(shop.getTokenCap())
                    .delayMinutes(delayMinutes)
                    .delayReason(delayReason)
                    .verified(shop.getVerified())
                    .phone(shop.getPhone())
                    .address(shop.getAddress())
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public DailyShopSummaryDto getDailySummary(UUID shopId, LocalDate date, User merchant) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));

        verifyShopOwnershipOrAdmin(shop, merchant);

        LocalDate queryDate = (date != null) ? date : LocalDate.now(ZoneOffset.UTC);
        Instant startOfDay = queryDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = queryDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Token> tokens = tokenRepository.findByShopIdAndJoinedAtBetween(shopId, startOfDay, endOfDay);

        int totalIssued = tokens.size();
        int totalCompleted = 0;
        int totalNoShows = 0;
        int totalCancelled = 0;
        int totalSkipped = 0;

        List<Double> waitTimes = new ArrayList<>();
        List<Double> serviceTimes = new ArrayList<>();
        Map<Integer, Integer> hourlyTraffic = new HashMap<>();

        for (Token t : tokens) {
            if (t.getStatus() == TokenStatus.COMPLETED) {
                totalCompleted++;
            } else if (t.getStatus() == TokenStatus.NO_SHOW) {
                totalNoShows++;
            } else if (t.getStatus() == TokenStatus.CANCELLED) {
                totalCancelled++;
            } else if (t.getStatus() == TokenStatus.SKIPPED) {
                totalSkipped++;
            }

            // Wait time: joinedAt to calledAt
            if (t.getCalledAt() != null && t.getJoinedAt() != null) {
                double waitMin = Duration.between(t.getJoinedAt(), t.getCalledAt()).toSeconds() / 60.0;
                if (waitMin >= 0) {
                    waitTimes.add(waitMin);
                }
            }

            // Service time: calledAt to servedAt or completedAt
            Instant finishTime = t.getServedAt() != null ? t.getServedAt() : t.getCompletedAt();
            if (finishTime != null && t.getCalledAt() != null) {
                double serviceMin = Duration.between(t.getCalledAt(), finishTime).toSeconds() / 60.0;
                if (serviceMin >= 0) {
                    serviceTimes.add(serviceMin);
                }
            }

            // Hourly bucket for peak traffic calculation
            if (t.getJoinedAt() != null) {
                int hour = t.getJoinedAt().atZone(ZoneOffset.UTC).getHour();
                hourlyTraffic.put(hour, hourlyTraffic.getOrDefault(hour, 0) + 1);
            }
        }

        double avgWaitTime = waitTimes.isEmpty() ? 0.0 :
                Math.round(waitTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 10.0) / 10.0;

        double avgServiceTime = serviceTimes.isEmpty() ? 0.0 :
                Math.round(serviceTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 10.0) / 10.0;

        // Peak hour determination
        String peakHour = "N/A";
        if (!hourlyTraffic.isEmpty()) {
            int maxHour = Collections.max(hourlyTraffic.entrySet(), Map.Entry.comparingByValue()).getKey();
            peakHour = String.format("%02d:00 - %02d:00", maxHour, (maxHour + 1) % 24);
        }

        // Operational hours calculation from shop status events
        double operationalHours = calculateOperationalHours(shopId, startOfDay, endOfDay);

        return DailyShopSummaryDto.builder()
                .shopId(shop.getId())
                .shopName(shop.getName())
                .date(queryDate)
                .totalTokensIssued(totalIssued)
                .totalTokensCompleted(totalCompleted)
                .totalNoShows(totalNoShows)
                .totalCancelled(totalCancelled)
                .totalSkipped(totalSkipped)
                .averageWaitTimeMinutes(avgWaitTime)
                .averageServiceTimeMinutes(avgServiceTime)
                .peakHour(peakHour)
                .totalOperationalHours(operationalHours)
                .build();
    }

    private double calculateOperationalHours(UUID shopId, Instant startOfDay, Instant endOfDay) {
        List<ShopStatusEvent> events = shopStatusEventRepository
                .findByShopIdAndCreatedAtBetweenOrderByCreatedAtAsc(shopId, startOfDay, endOfDay);

        if (events.isEmpty()) {
            return 0.0;
        }

        long activeSeconds = 0;
        Instant lastOpenTime = null;

        for (ShopStatusEvent event : events) {
            ShopStatus status = event.getToStatus();
            if (status == ShopStatus.OPEN || status == ShopStatus.BUSY) {
                if (lastOpenTime == null) {
                    lastOpenTime = event.getCreatedAt();
                }
            } else if (status == ShopStatus.CLOSED || status == ShopStatus.PAUSED) {
                if (lastOpenTime != null) {
                    activeSeconds += Duration.between(lastOpenTime, event.getCreatedAt()).toSeconds();
                    lastOpenTime = null;
                }
            }
        }

        // If still open at end of range
        if (lastOpenTime != null) {
            activeSeconds += Duration.between(lastOpenTime, Instant.now().isBefore(endOfDay) ? Instant.now() : endOfDay).toSeconds();
        }

        return Math.round((activeSeconds / 3600.0) * 10.0) / 10.0;
    }

    private void verifyShopOwnershipOrAdmin(Shop shop, User user) {
        if (user == null) {
            throw new ForbiddenException("Authentication required.");
        }
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        UUID ownerId = (shop.getOwner() != null) ? shop.getOwner().getId() : null;
        if (!user.getId().equals(ownerId)) {
            throw new ForbiddenException("You do not have access to view analytics for this shop.");
        }
    }

    private String getCrowdLabel(Integer level) {
        if (level == null) return "Empty (< 5 min wait)";
        return switch (level) {
            case 2 -> "Moderate (5–15 min wait)";
            case 3 -> "Crowded (15–35 min wait)";
            case 4 -> "Extremely Busy (> 35 min wait)";
            default -> "Empty (< 5 min wait)";
        };
    }
}

