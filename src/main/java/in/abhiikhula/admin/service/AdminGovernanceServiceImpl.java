package in.abhiikhula.admin.service;

import in.abhiikhula.admin.dto.AdminShopModerationDto;
import in.abhiikhula.admin.dto.AdminUserModerationDto;
import in.abhiikhula.admin.dto.ForceShopStatusRequest;
import in.abhiikhula.admin.dto.PlatformMetricsDto;
import in.abhiikhula.category.model.Category;
import in.abhiikhula.category.repository.CategoryRepository;
import in.abhiikhula.common.exception.BadRequestException;
import in.abhiikhula.common.exception.ForbiddenException;
import in.abhiikhula.common.exception.ResourceNotFoundException;
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
import in.abhiikhula.user.repository.UserRepository;
import in.abhiikhula.zone.model.Zone;
import in.abhiikhula.zone.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminGovernanceServiceImpl implements AdminGovernanceService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final QueueSessionRepository queueSessionRepository;
    private final TokenRepository tokenRepository;
    private final ZoneRepository zoneRepository;
    private final CategoryRepository categoryRepository;
    private final ShopStatusEventRepository shopStatusEventRepository;

    @Override
    @Transactional(readOnly = true)
    public PlatformMetricsDto getPlatformMetrics(User admin) {
        verifyAdmin(admin);

        Instant start = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = start.plus(1, ChronoUnit.DAYS);

        long totalUsers = userRepository.countByDeletedFalse();
        long totalCustomers = userRepository.countByRole(UserRole.CUSTOMER);
        long totalShopOwners = userRepository.countByRole(UserRole.SHOP_OWNER);
        long blockedUsers = userRepository.countByBlockedTrue();

        long totalShops = shopRepository.count();
        long verifiedShops = shopRepository.countByVerified(true);
        long unverifiedShops = shopRepository.countByVerified(false);
        long activeShops = shopRepository.countByActive(true);
        long suspendedShops = shopRepository.countByActive(false);

        long activeQueues = queueSessionRepository.countByStatusIn(List.of(SessionStatus.ACTIVE, SessionStatus.PAUSED));
        long tokensIssuedToday = tokenRepository.countByJoinedAtBetween(start, end);

        long activeZones = zoneRepository.countByActive(true);
        long activeCategories = categoryRepository.countByActive(true);

        return PlatformMetricsDto.builder()
                .totalUsers(totalUsers)
                .totalCustomers(totalCustomers)
                .totalShopOwners(totalShopOwners)
                .blockedUsers(blockedUsers)
                .totalShops(totalShops)
                .verifiedShops(verifiedShops)
                .unverifiedShops(unverifiedShops)
                .activeShops(activeShops)
                .suspendedShops(suspendedShops)
                .activeQueuesNow(activeQueues)
                .tokensIssuedToday(tokensIssuedToday)
                .activeZones(activeZones)
                .activeCategories(activeCategories)
                .build();
    }

    @Override
    @Transactional
    public AdminShopModerationDto toggleShopActive(UUID shopId, User admin) {
        verifyAdmin(admin);

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));

        boolean newActiveState = !Boolean.TRUE.equals(shop.getActive());
        shop.setActive(newActiveState);

        // If suspending shop, close active queue sessions
        if (!newActiveState) {
            Optional<QueueSession> activeSessionOpt = queueSessionRepository
                    .findByShopIdAndStatusIn(shop.getId(), List.of(SessionStatus.ACTIVE, SessionStatus.PAUSED));
            if (activeSessionOpt.isPresent()) {
                QueueSession session = activeSessionOpt.get();
                session.setStatus(SessionStatus.CLOSED);
                session.setClosedAt(Instant.now());
                queueSessionRepository.save(session);

                // Cancel pending waiting tokens
                List<Token> waiting = tokenRepository.findBySessionIdAndStatusOrderByTokenNumberAsc(session.getId(), TokenStatus.WAITING);
                for (Token t : waiting) {
                    t.setStatus(TokenStatus.CANCELLED);
                    t.setCancellationReason("Shop listing suspended by admin governance.");
                    tokenRepository.save(t);
                }
            }
        }

        Shop updated = shopRepository.save(shop);
        String action = newActiveState ? "Shop reactivated successfully" : "Shop suspended by admin governance";
        log.info("Admin {} changed shop {} active status to {}", admin.getId(), shopId, newActiveState);

        return toShopModerationDto(updated, action);
    }

    @Override
    @Transactional
    public AdminShopModerationDto forceShopStatus(UUID shopId, ForceShopStatusRequest request, User admin) {
        verifyAdmin(admin);

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));

        ShopStatus oldStatus = shop.getStatus();
        ShopStatus newStatus = request.getStatus();

        if (request.getCrowdLevel() != null) {
            shop.setCrowdLevel(request.getCrowdLevel());
        }
        shop.setStatus(newStatus);
        shop.setLastStatusUpdateAt(Instant.now());

        ShopStatusEvent event = ShopStatusEvent.builder()
                .shop(shop)
                .fromStatus(oldStatus)
                .toStatus(newStatus)
                .crowdLevel(shop.getCrowdLevel())
                .source("ADMIN_OVERRIDE")
                .note(request.getReason() != null ? request.getReason().trim() : "Admin status override")
                .build();
        shopStatusEventRepository.save(event);

        Shop updated = shopRepository.save(shop);
        log.info("Admin {} force changed shop {} status from {} to {}. Reason: {}",
                admin.getId(), shopId, oldStatus, newStatus, request.getReason());

        return toShopModerationDto(updated, "Shop status force-updated to " + newStatus);
    }

    @Override
    @Transactional
    public AdminUserModerationDto blockUser(UUID userId, User admin) {
        verifyAdmin(admin);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getRole() == UserRole.ADMIN) {
            throw new BadRequestException("Cannot block administrator accounts.");
        }

        user.setBlocked(true);
        userRepository.save(user);

        // Cancel any active tokens held by this user
        List<Token> activeTokens = tokenRepository.findByUserIdAndStatusInOrderByJoinedAtDesc(
                user.getId(),
                List.of(TokenStatus.WAITING, TokenStatus.CALLED)
        );
        for (Token t : activeTokens) {
            t.setStatus(TokenStatus.CANCELLED);
            t.setCancellationReason("User account suspended by administrator.");
            tokenRepository.save(t);
        }

        log.info("Admin {} BLOCKED user {}", admin.getId(), userId);
        return toUserModerationDto(user, "User blocked successfully and active tokens cancelled.");
    }

    @Override
    @Transactional
    public AdminUserModerationDto unblockUser(UUID userId, User admin) {
        verifyAdmin(admin);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setBlocked(false);
        userRepository.save(user);

        log.info("Admin {} UNBLOCKED user {}", admin.getId(), userId);
        return toUserModerationDto(user, "User unblocked successfully.");
    }

    @Override
    @Transactional
    public boolean toggleZoneActive(UUID zoneId, User admin) {
        verifyAdmin(admin);

        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", zoneId));

        zone.setActive(!Boolean.TRUE.equals(zone.getActive()));
        zoneRepository.save(zone);
        log.info("Admin {} toggled zone {} active state to {}", admin.getId(), zoneId, zone.getActive());
        return zone.getActive();
    }

    @Override
    @Transactional
    public boolean toggleCategoryActive(UUID categoryId, User admin) {
        verifyAdmin(admin);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        category.setActive(!Boolean.TRUE.equals(category.getActive()));
        categoryRepository.save(category);
        log.info("Admin {} toggled category {} active state to {}", admin.getId(), categoryId, category.getActive());
        return category.getActive();
    }

    private void verifyAdmin(User user) {
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Only administrators have access to this resource.");
        }
    }

    private AdminShopModerationDto toShopModerationDto(Shop shop, String message) {
        return AdminShopModerationDto.builder()
                .shopId(shop.getId())
                .name(shop.getName())
                .status(shop.getStatus())
                .active(shop.getActive())
                .verified(shop.getVerified())
                .ownerId(shop.getOwner() != null ? shop.getOwner().getId() : null)
                .ownerPhone(shop.getOwner() != null ? shop.getOwner().getPhone() : null)
                .message(message)
                .build();
    }

    private AdminUserModerationDto toUserModerationDto(User user, String message) {
        return AdminUserModerationDto.builder()
                .userId(user.getId())
                .phone(user.getPhone())
                .name(user.getName())
                .role(user.getRole())
                .blocked(user.getBlocked())
                .deleted(user.getDeleted())
                .message(message)
                .build();
    }
}

