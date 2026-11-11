package in.abhiikhula.shop.service;

import in.abhiikhula.common.exception.ForbiddenException;
import in.abhiikhula.common.exception.ResourceNotFoundException;
import in.abhiikhula.notification.model.NotificationTemplate;
import in.abhiikhula.notification.service.NotificationService;
import in.abhiikhula.queue.model.QueueSession;
import in.abhiikhula.queue.model.SessionStatus;
import in.abhiikhula.queue.model.Token;
import in.abhiikhula.queue.model.TokenStatus;
import in.abhiikhula.queue.repository.QueueSessionRepository;
import in.abhiikhula.queue.repository.TokenRepository;
import in.abhiikhula.shop.dto.ShopStatusResponseDto;
import in.abhiikhula.shop.dto.StatusEventHistoryDto;
import in.abhiikhula.shop.dto.UpdateStatusRequest;
import in.abhiikhula.shop.mapper.ShopStatusMapper;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopStatusServiceImpl implements ShopStatusService {

    private final ShopRepository shopRepository;
    private final ShopStatusEventRepository shopStatusEventRepository;
    private final ShopStatusMapper shopStatusMapper;
    private final QueueSessionRepository queueSessionRepository;
    private final TokenRepository tokenRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ShopStatusResponseDto updateStatus(UUID shopId, UpdateStatusRequest request, User currentUser) {
        log.info("Status update requested for shopId: {} by user: {}", shopId, currentUser.getId());

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));

        // Ownership & authorization check (BRD: Only verified owner or admin can update status)
        boolean isOwner = shop.getOwner() != null && shop.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            log.warn("Unauthorized attempt to update status of shop {} by user {}", shopId, currentUser.getId());
            throw new ForbiddenException("You are not authorized to update status for this shop");
        }

        ShopStatus fromStatus = shop.getStatus();
        ShopStatus toStatus = request.getStatus();

        // Update shop live availability fields
        shop.setStatus(toStatus);
        shop.setCrowdLevel(request.getCrowdLevel());
        shop.setLastStatusUpdateAt(Instant.now());
        Shop updatedShop = shopRepository.save(shop);

        // Record immutable audit event
        ShopStatusEvent event = ShopStatusEvent.builder()
                .shop(updatedShop)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .crowdLevel(request.getCrowdLevel())
                .source(request.getSource() != null ? request.getSource() : "OWNER_APP")
                .lat(request.getLat())
                .lng(request.getLng())
                .note(request.getNote())
                .build();
        shopStatusEventRepository.save(event);

        // When merchant manually closes shop, gracefully close active queue session & notify waiting customers
        if (toStatus == ShopStatus.CLOSED) {
            queueSessionRepository.findByShopIdAndStatusIn(shopId, List.of(SessionStatus.ACTIVE, SessionStatus.PAUSED))
                    .ifPresent(session -> {
                        session.setStatus(SessionStatus.CLOSED);
                        session.setClosedAt(Instant.now());
                        queueSessionRepository.save(session);

                        List<Token> pendingTokens = tokenRepository.findBySessionIdAndStatusIn(
                                session.getId(),
                                List.of(TokenStatus.WAITING, TokenStatus.CALLED)
                        );
                        for (Token t : pendingTokens) {
                            t.setStatus(TokenStatus.CANCELLED);
                            t.setCancelledAt(Instant.now());
                            t.setCancellationReason("Shop closed manually by merchant");
                            tokenRepository.save(t);

                            notificationService.sendQueueNotification(t, NotificationTemplate.SHOP_CLOSED, "Dukandaar ne dukaan band kar di hai.");
                        }
                        log.info("Closed active queue session {} for shop {} and cancelled {} pending tokens",
                                session.getId(), shopId, pendingTokens.size());
                    });
        }

        log.info("Shop {} status updated: {} -> {} (Crowd: {})", shopId, fromStatus, toStatus, request.getCrowdLevel());

        List<ShopStatusEvent> recentEvents = shopStatusEventRepository.findTop10ByShopIdOrderByCreatedAtDesc(shopId);
        List<StatusEventHistoryDto> eventDtos = shopStatusMapper.toEventDtoList(recentEvents);

        return shopStatusMapper.toStatusResponseDto(updatedShop, request.getNote(), eventDtos);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopStatusResponseDto getStatus(UUID shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));

        List<ShopStatusEvent> recentEvents = shopStatusEventRepository.findTop10ByShopIdOrderByCreatedAtDesc(shopId);
        List<StatusEventHistoryDto> eventDtos = shopStatusMapper.toEventDtoList(recentEvents);

        String latestNote = recentEvents.isEmpty() ? null : recentEvents.get(0).getNote();

        return shopStatusMapper.toStatusResponseDto(shop, latestNote, eventDtos);
    }
}

