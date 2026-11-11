package in.abhiikhula.queue.service;

import in.abhiikhula.common.exception.BadRequestException;
import in.abhiikhula.common.exception.ForbiddenException;
import in.abhiikhula.common.exception.ResourceNotFoundException;
import in.abhiikhula.queue.dto.CancelTokenRequest;
import in.abhiikhula.queue.dto.JoinQueueRequest;
import in.abhiikhula.queue.dto.QueueStatusResponseDto;
import in.abhiikhula.queue.dto.TokenResponseDto;
import in.abhiikhula.queue.mapper.QueueMapper;
import in.abhiikhula.queue.model.*;
import in.abhiikhula.queue.repository.QueueSessionRepository;
import in.abhiikhula.queue.repository.TokenEventRepository;
import in.abhiikhula.queue.repository.TokenRepository;
import in.abhiikhula.shop.model.Shop;
import in.abhiikhula.shop.model.ShopStatus;
import in.abhiikhula.shop.repository.ShopRepository;
import in.abhiikhula.user.model.User;
import in.abhiikhula.user.model.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueServiceImpl implements QueueService {

    private final ShopRepository shopRepository;
    private final QueueSessionRepository queueSessionRepository;
    private final TokenRepository tokenRepository;
    private final TokenEventRepository tokenEventRepository;
    private final QueueMapper queueMapper;
    private final in.abhiikhula.notification.service.NotificationService notificationService;
    private final in.abhiikhula.queue.sse.QueueSseBroadcaster queueSseBroadcaster;
    private final in.abhiikhula.platformconfig.service.PlatformConfigService platformConfigService;
    private final in.abhiikhula.billing.service.BillingService billingService;

    @Override
    @Transactional
    public TokenResponseDto joinQueue(UUID shopId, JoinQueueRequest request, User currentUser) {
        log.info("User {} attempting to join queue for shop {}", currentUser.getId(), shopId);

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));

        if (!Boolean.TRUE.equals(shop.getActive())) {
            throw new BadRequestException("This shop is currently inactive");
        }

        // Shop status check (Must be OPEN or BUSY to take tokens)
        if (shop.getStatus() == ShopStatus.CLOSED) {
            throw new BadRequestException("Shop is currently CLOSED. Tokens are not being issued.");
        }
        if (shop.getStatus() == ShopStatus.PAUSED) {
            throw new BadRequestException("Queue is temporarily PAUSED by merchant. Please check back shortly.");
        }

        // Anti-spam check: One active token per user per shop (BRD: Virtual Queue anti-flooding)
        boolean hasActive = tokenRepository.existsByShopIdAndUserIdAndStatusIn(
                shopId,
                currentUser.getId(),
                List.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.SERVING)
        );
        if (hasActive) {
            throw new BadRequestException("You already have an active token in this shop's queue.");
        }

        // Anti-hoarding check: Max active tokens per user across all shops (BRD Section 26: max_active_tokens_user)
        int maxTokens = platformConfigService.getIntConfig("max_active_tokens_user", 3);
        long activeUserTokens = tokenRepository.countByUserIdAndStatusIn(
                currentUser.getId(),
                List.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.SERVING)
        );
        if (activeUserTokens >= maxTokens) {
            throw new BadRequestException("You have reached the maximum active tokens limit (" + maxTokens + ") across shops.");
        }

        // Get or Auto-create today's active session
        QueueSession session = queueSessionRepository
                .findByShopIdAndStatusIn(shopId, List.of(SessionStatus.ACTIVE))
                .orElseGet(() -> {
                    log.info("No active session found for shop {}. Creating fresh session for today.", shopId);
                    return queueSessionRepository.save(QueueSession.builder()
                            .shop(shop)
                            .sessionDate(LocalDate.now())
                            .status(SessionStatus.ACTIVE)
                            .currentTokenNumber(0)
                            .totalTokensIssued(0)
                            .openedAt(Instant.now())
                            .build());
                });

        // Pause Check (BRD FR-707)
        if (session.getStatus() == SessionStatus.PAUSED) {
            String reason = session.getPauseReason() != null ? session.getPauseReason() : "Break";
            throw new BadRequestException("Queue is currently PAUSED (" + reason + "). New tokens are temporarily disabled.");
        }

        // Token Cap Check (BRD Section 29)
        int requestedSize = (request.getGroupSize() != null && request.getGroupSize() >= 1) ? request.getGroupSize() : 1;
        if (session.getTotalTokensIssued() + requestedSize > shop.getTokenCap()) {
            throw new BadRequestException("Daily token limit (" + shop.getTokenCap() + ") will be exceeded. Available slots: "
                    + (shop.getTokenCap() - session.getTotalTokensIssued()));
        }

        // M9 Subscription Quota Check (BRD Section 20, FR-905)
        if (!billingService.canIssueToken(shopId)) {
            throw new BadRequestException("Daily free token limit reached for this shop. Please upgrade to Live Pro.");
        }

        long waitingAhead = tokenRepository.countBySessionIdAndStatus(session.getId(), TokenStatus.WAITING);
        int serviceDuration = shop.getCategory() != null ? shop.getCategory().getDefaultServiceMin() : 15;
        int delay = session.getDelayMinutes() != null ? session.getDelayMinutes() : 0;

        String customerPhone = (request.getCustomerPhone() != null && !request.getCustomerPhone().isBlank())
                ? request.getCustomerPhone().trim()
                : currentUser.getPhone();

        String primaryCustomerName = (request.getCustomerName() != null && !request.getCustomerName().isBlank())
                ? request.getCustomerName().trim()
                : (currentUser.getName() != null ? currentUser.getName() : "Customer");

        UUID groupId = requestedSize > 1 ? UUID.randomUUID() : null;
        Token primaryToken = null;

        for (int i = 0; i < requestedSize; i++) {
            int nextTokenNumber = session.getTotalTokensIssued() + 1;
            session.setTotalTokensIssued(nextTokenNumber);
            queueSessionRepository.save(session);

            int estimatedWait = (int) ((waitingAhead + i) * serviceDuration) + delay;
            String memberName = primaryCustomerName;
            if (request.getMemberNames() != null && i < request.getMemberNames().size() && request.getMemberNames().get(i) != null && !request.getMemberNames().get(i).isBlank()) {
                memberName = request.getMemberNames().get(i).trim();
            } else if (i > 0) {
                memberName = primaryCustomerName + " (Guest " + (i + 1) + ")";
            }

            Token token = Token.builder()
                    .session(session)
                    .shop(shop)
                    .user(currentUser)
                    .tokenNumber(nextTokenNumber)
                    .displayToken("#" + nextTokenNumber)
                    .status(TokenStatus.WAITING)
                    .source(request.getSource() != null ? request.getSource() : "APP")
                    .customerName(memberName)
                    .customerPhone(customerPhone)
                    .estimatedWaitMin(Math.max(estimatedWait, 5))
                    .joinedAt(Instant.now())
                    .groupId(groupId)
                    .groupSize(requestedSize)
                    .groupMemberIndex(i + 1)
                    .build();

            Token saved = tokenRepository.save(token);
            billingService.recordTokenIssued(shopId);

            tokenEventRepository.save(TokenEvent.builder()
                    .token(saved)
                    .fromStatus(TokenStatus.WAITING)
                    .toStatus(TokenStatus.WAITING)
                    .actorUser(currentUser)
                    .note(requestedSize > 1 ? "Group Token (" + (i + 1) + "/" + requestedSize + ") joined queue via " + saved.getSource()
                            : "Token joined queue via " + saved.getSource())
                    .build());

            if (i == 0) {
                primaryToken = saved;
            }
        }

        log.info("Issued {} token(s) (groupId={}) for shop {} to user {}", requestedSize, groupId, shopId, currentUser.getId());

        triggerSseBroadcast(shopId);

        return queueMapper.toTokenResponseDto(primaryToken, (int) waitingAhead);
    }

    @Override
    @Transactional
    public TokenResponseDto issueWalkInToken(UUID shopId, in.abhiikhula.queue.dto.IssueWalkInTokenRequest request, User currentUser) {
        log.info("Merchant {} issuing walk-in token for shop {}", currentUser.getId(), shopId);
        verifyShopOwnership(shopId, currentUser);

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));

        if (!Boolean.TRUE.equals(shop.getActive())) {
            throw new BadRequestException("This shop is currently inactive");
        }
        if (shop.getStatus() == ShopStatus.CLOSED) {
            throw new BadRequestException("Shop is currently CLOSED. Cannot issue walk-in tokens.");
        }
        if (shop.getStatus() == ShopStatus.PAUSED) {
            throw new BadRequestException("Queue is temporarily PAUSED. Cannot issue walk-in tokens.");
        }

        // Feature flag kill-switch check (BRD Section 26: flags.walk_in)
        if (!platformConfigService.isFlagEnabled("flags.walk_in", true)) {
            throw new BadRequestException("Offline walk-in token issuance is currently disabled by administrator.");
        }

        QueueSession session = queueSessionRepository
                .findByShopIdAndStatusIn(shopId, List.of(SessionStatus.ACTIVE))
                .orElseGet(() -> {
                    log.info("No active session found for shop {}. Creating fresh session for today.", shopId);
                    return queueSessionRepository.save(QueueSession.builder()
                            .shop(shop)
                            .sessionDate(LocalDate.now())
                            .status(SessionStatus.ACTIVE)
                            .currentTokenNumber(0)
                            .totalTokensIssued(0)
                            .openedAt(Instant.now())
                            .build());
                });

        if (session.getStatus() == SessionStatus.PAUSED) {
            String reason = session.getPauseReason() != null ? session.getPauseReason() : "Break";
            throw new BadRequestException("Queue is currently PAUSED (" + reason + "). Walk-in tokens disabled.");
        }

        if (session.getTotalTokensIssued() >= shop.getTokenCap()) {
            throw new BadRequestException("Daily token limit (" + shop.getTokenCap() + ") reached for this shop.");
        }

        // M9 Subscription Quota Check (BRD Section 20, FR-905)
        if (!billingService.canIssueToken(shopId)) {
            throw new BadRequestException("Daily free token limit reached for this shop. Please upgrade to Live Pro.");
        }

        int nextTokenNumber = session.getTotalTokensIssued() + 1;
        session.setTotalTokensIssued(nextTokenNumber);
        queueSessionRepository.save(session);

        long waitingAhead = tokenRepository.countBySessionIdAndStatus(session.getId(), TokenStatus.WAITING);
        int serviceDuration = shop.getCategory() != null ? shop.getCategory().getDefaultServiceMin() : 15;
        int delay = session.getDelayMinutes() != null ? session.getDelayMinutes() : 0;
        int estimatedWait = (int) (waitingAhead * serviceDuration) + delay;

        String customerName = (request != null && request.getCustomerName() != null && !request.getCustomerName().isBlank())
                ? request.getCustomerName().trim()
                : "Walk-in #" + nextTokenNumber;

        String customerPhone = (request != null && request.getCustomerPhone() != null && !request.getCustomerPhone().isBlank())
                ? request.getCustomerPhone().trim()
                : null;

        Token token = Token.builder()
                .session(session)
                .shop(shop)
                .user(null)
                .tokenNumber(nextTokenNumber)
                .displayToken("#" + nextTokenNumber)
                .status(TokenStatus.WAITING)
                .source("WALK_IN")
                .customerName(customerName)
                .customerPhone(customerPhone)
                .estimatedWaitMin(Math.max(estimatedWait, 5))
                .joinedAt(Instant.now())
                .build();

        Token savedToken = tokenRepository.save(token);
        billingService.recordTokenIssued(shopId);

        String note = (request != null && request.getNote() != null && !request.getNote().isBlank())
                ? request.getNote().trim()
                : "Offline Walk-in Token issued by merchant";

        tokenEventRepository.save(TokenEvent.builder()
                .token(savedToken)
                .fromStatus(TokenStatus.WAITING)
                .toStatus(TokenStatus.WAITING)
                .actorUser(currentUser)
                .note(note)
                .build());

        log.info("Walk-in Token {} (#{}) issued for shop {} by merchant {}", savedToken.getId(), nextTokenNumber, shopId, currentUser.getId());

        triggerSseBroadcast(shopId);

        return queueMapper.toTokenResponseDto(savedToken, (int) waitingAhead);
    }

    @Override
    @Transactional
    public TokenResponseDto callNext(UUID shopId, User currentUser) {
        verifyShopOwnership(shopId, currentUser);

        QueueSession session = queueSessionRepository
                .findByShopIdAndStatusIn(shopId, List.of(SessionStatus.ACTIVE))
                .orElseThrow(() -> new BadRequestException("No active queue session found for this shop"));

        Token nextToken = tokenRepository
                .findFirstBySessionIdAndStatusOrderByTokenNumberAsc(session.getId(), TokenStatus.WAITING)
                .orElseThrow(() -> new BadRequestException("No customers currently waiting in the queue."));

        TokenStatus fromStatus = nextToken.getStatus();
        nextToken.setStatus(TokenStatus.CALLED);
        nextToken.setCalledAt(Instant.now());
        tokenRepository.save(nextToken);

        session.setCurrentTokenNumber(nextToken.getTokenNumber());
        queueSessionRepository.save(session);

        // Notify Called Customer (BRD FR-403: "Your number now")
        notificationService.sendQueueNotification(
                nextToken,
                in.abhiikhula.notification.model.NotificationTemplate.YOUR_TURN,
                null
        );

        // Notify Upcoming Customer (BRD FR-402: "Two ahead" reminder)
        List<Token> waitingTokens = tokenRepository.findBySessionIdAndStatusOrderByTokenNumberAsc(session.getId(), TokenStatus.WAITING);
        if (!waitingTokens.isEmpty()) {
            Token upcoming = waitingTokens.size() >= 2 ? waitingTokens.get(1) : waitingTokens.get(0);
            notificationService.sendQueueNotification(
                    upcoming,
                    in.abhiikhula.notification.model.NotificationTemplate.TWO_AHEAD,
                    null
            );
        }

        tokenEventRepository.save(TokenEvent.builder()
                .token(nextToken)
                .fromStatus(fromStatus)
                .toStatus(TokenStatus.CALLED)
                .actorUser(currentUser)
                .note("Merchant called token #" + nextToken.getTokenNumber())
                .build());

        log.info("Shop {}: Called next token #{}", shopId, nextToken.getTokenNumber());

        triggerSseBroadcast(shopId);

        return queueMapper.toTokenResponseDto(nextToken, 0);
    }

    @Override
    @Transactional
    public TokenResponseDto completeToken(UUID shopId, UUID tokenId, User currentUser) {
        verifyShopOwnership(shopId, currentUser);

        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token", "id", tokenId));

        if (!token.getShop().getId().equals(shopId)) {
            throw new BadRequestException("Token does not belong to this shop");
        }

        TokenStatus fromStatus = token.getStatus();
        token.setStatus(TokenStatus.COMPLETED);
        token.setCompletedAt(Instant.now());
        tokenRepository.save(token);

        tokenEventRepository.save(TokenEvent.builder()
                .token(token)
                .fromStatus(fromStatus)
                .toStatus(TokenStatus.COMPLETED)
                .actorUser(currentUser)
                .note("Service completed by merchant")
                .build());

        log.info("Token {} marked COMPLETED for shop {}", tokenId, shopId);

        triggerSseBroadcast(shopId);

        return queueMapper.toTokenResponseDto(token, 0);
    }

    @Override
    @Transactional
    public TokenResponseDto markNoShow(UUID shopId, UUID tokenId, User currentUser) {
        verifyShopOwnership(shopId, currentUser);

        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token", "id", tokenId));

        if (!token.getShop().getId().equals(shopId)) {
            throw new BadRequestException("Token does not belong to this shop");
        }

        TokenStatus fromStatus = token.getStatus();
        token.setStatus(TokenStatus.NO_SHOW);
        token.setCompletedAt(Instant.now());
        tokenRepository.save(token);

        tokenEventRepository.save(TokenEvent.builder()
                .token(token)
                .fromStatus(fromStatus)
                .toStatus(TokenStatus.NO_SHOW)
                .actorUser(currentUser)
                .note("Customer did not show up (NO_SHOW)")
                .build());

        log.info("Token {} marked NO_SHOW for shop {}", tokenId, shopId);

        triggerSseBroadcast(shopId);

        return queueMapper.toTokenResponseDto(token, 0);
    }

    @Override
    @Transactional
    public TokenResponseDto skipToken(UUID shopId, UUID tokenId, User currentUser) {
        verifyShopOwnership(shopId, currentUser);

        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token", "id", tokenId));

        if (!token.getShop().getId().equals(shopId)) {
            throw new BadRequestException("Token does not belong to this shop");
        }

        if (token.getStatus() != TokenStatus.WAITING && token.getStatus() != TokenStatus.CALLED) {
            throw new BadRequestException("Only WAITING or CALLED tokens can be skipped");
        }

        TokenStatus fromStatus = token.getStatus();
        token.setStatus(TokenStatus.SKIPPED);
        token.setCompletedAt(Instant.now());
        tokenRepository.save(token);

        tokenEventRepository.save(TokenEvent.builder()
                .token(token)
                .fromStatus(fromStatus)
                .toStatus(TokenStatus.SKIPPED)
                .actorUser(currentUser)
                .note("Customer skipped by merchant")
                .build());

        log.info("Token {} marked SKIPPED for shop {}", tokenId, shopId);

        triggerSseBroadcast(shopId);

        return queueMapper.toTokenResponseDto(token, 0);
    }

    @Override
    @Transactional
    public QueueStatusResponseDto updateQueueDelay(UUID shopId, in.abhiikhula.queue.dto.UpdateQueueDelayRequest request, User currentUser) {
        verifyShopOwnership(shopId, currentUser);

        QueueSession session = queueSessionRepository
                .findByShopIdAndStatusIn(shopId, List.of(SessionStatus.ACTIVE, SessionStatus.PAUSED))
                .orElseThrow(() -> new BadRequestException("No active queue session found for this shop"));

        session.setDelayMinutes(request.getDelayMinutes());
        session.setDelayReason(request.getReason());
        queueSessionRepository.save(session);

        // Broadcast delay alert to all active waiting tokens
        List<Token> waitingTokens = tokenRepository.findBySessionIdAndStatusOrderByTokenNumberAsc(session.getId(), TokenStatus.WAITING);
        for (Token t : waitingTokens) {
            notificationService.sendQueueNotification(
                    t,
                    in.abhiikhula.notification.model.NotificationTemplate.DELAY_ALERT,
                    "+" + request.getDelayMinutes() + " min delay. " + (request.getReason() != null ? request.getReason() : "")
            );
        }

        log.info("Shop {} queue delay updated: +{} min, reason: {}", shopId, request.getDelayMinutes(), request.getReason());

        triggerSseBroadcast(shopId);

        return getQueueStatus(shopId);
    }

    @Override
    @Transactional
    public TokenResponseDto cancelToken(UUID tokenId, CancelTokenRequest request, User currentUser) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token", "id", tokenId));

        boolean isCustomer = token.getUser() != null && token.getUser().getId().equals(currentUser.getId());
        boolean isOwner = token.getShop().getOwner() != null && token.getShop().getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isCustomer && !isOwner && !isAdmin) {
            throw new ForbiddenException("You are not authorized to cancel this token");
        }

        if (token.getStatus() == TokenStatus.COMPLETED || token.getStatus() == TokenStatus.CANCELLED) {
            throw new BadRequestException("Cannot cancel a token that is already " + token.getStatus());
        }

        TokenStatus fromStatus = token.getStatus();
        token.setStatus(TokenStatus.CANCELLED);
        token.setCancelledAt(Instant.now());
        token.setCancellationReason(request != null ? request.getReason() : "Cancelled by user");
        tokenRepository.save(token);

        tokenEventRepository.save(TokenEvent.builder()
                .token(token)
                .fromStatus(fromStatus)
                .toStatus(TokenStatus.CANCELLED)
                .actorUser(currentUser)
                .note("Cancelled: " + token.getCancellationReason())
                .build());

        log.info("Token {} CANCELLED by user {}", tokenId, currentUser.getId());

        triggerSseBroadcast(token.getShop().getId());

        return queueMapper.toTokenResponseDto(token, 0);
    }

    @Override
    @Transactional
    public TokenResponseDto checkIn(UUID tokenId, in.abhiikhula.queue.dto.CheckInRequest request, User currentUser) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token", "id", tokenId));

        boolean isCustomer = token.getUser() != null && token.getUser().getId().equals(currentUser.getId());
        boolean isOwner = token.getShop().getOwner() != null && token.getShop().getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isCustomer && !isOwner && !isAdmin) {
            throw new ForbiddenException("You are not authorized to check in for this token");
        }

        if (token.getStatus() != TokenStatus.WAITING && token.getStatus() != TokenStatus.CALLED) {
            throw new BadRequestException("Check-in is only allowed for active WAITING or CALLED tokens (current status: " + token.getStatus() + ")");
        }

        if (Boolean.TRUE.equals(token.getCheckedIn())) {
            return queueMapper.toTokenResponseDto(token, 0);
        }

        Shop shop = token.getShop();
        if (shop.getLat() != null && shop.getLng() != null && request.getLat() != null && request.getLng() != null) {
            double distMeters = calculateHaversineKm(
                    request.getLat().doubleValue(), request.getLng().doubleValue(),
                    shop.getLat().doubleValue(), shop.getLng().doubleValue()
            ) * 1000.0;

            // 150m allowance to accommodate mobile GPS drift in dense market streets (BRD FR-307)
            if (distMeters > 150.0) {
                throw new BadRequestException("Check-in failed: You are " + Math.round(distMeters) + " m away from the shop. You must be within 100m of the shop to check in (BRD FR-307).");
            }
        }

        token.setCheckedIn(true);
        token.setCheckedInAt(Instant.now());
        token.setCheckinLat(request.getLat());
        token.setCheckinLng(request.getLng());
        tokenRepository.save(token);

        tokenEventRepository.save(TokenEvent.builder()
                .token(token)
                .fromStatus(token.getStatus())
                .toStatus(token.getStatus())
                .actorUser(currentUser)
                .note("Customer checked in at shop")
                .build());

        log.info("Token {} CHECKED_IN successfully by user {}", tokenId, currentUser.getId());

        return queueMapper.toTokenResponseDto(token, 0);
    }

    @Override
    @Transactional
    public TokenResponseDto reQueue(UUID tokenId, User currentUser) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token", "id", tokenId));

        boolean isCustomer = token.getUser() != null && token.getUser().getId().equals(currentUser.getId());
        boolean isOwner = token.getShop().getOwner() != null && token.getShop().getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isCustomer && !isOwner && !isAdmin) {
            throw new ForbiddenException("You are not authorized to re-queue this token");
        }

        if (token.getStatus() != TokenStatus.SKIPPED && token.getStatus() != TokenStatus.NO_SHOW) {
            throw new BadRequestException("Only SKIPPED or NO_SHOW tokens can be re-queued (current: " + token.getStatus() + ")");
        }

        if (Boolean.TRUE.equals(token.getRequeued())) {
            throw new BadRequestException("This token has already been re-queued once. Re-queue is permitted only once per token (BRD FR-309).");
        }

        QueueSession session = queueSessionRepository
                .findByShopIdAndStatusIn(token.getShop().getId(), List.of(SessionStatus.ACTIVE))
                .orElseThrow(() -> new BadRequestException("Queue is not currently active. Cannot re-queue right now."));

        int nextTokenNumber = session.getTotalTokensIssued() + 1;
        session.setTotalTokensIssued(nextTokenNumber);
        queueSessionRepository.save(session);

        TokenStatus fromStatus = token.getStatus();
        token.setStatus(TokenStatus.WAITING);
        token.setTokenNumber(nextTokenNumber);
        token.setDisplayToken("#" + nextTokenNumber);
        token.setRequeued(true);
        token.setCheckedIn(false);
        token.setCheckedInAt(null);
        token.setJoinedAt(Instant.now());
        tokenRepository.save(token);

        tokenEventRepository.save(TokenEvent.builder()
                .token(token)
                .fromStatus(fromStatus)
                .toStatus(TokenStatus.WAITING)
                .actorUser(currentUser)
                .note("Token re-queued at the back as #" + nextTokenNumber + " (BRD FR-309)")
                .build());

        log.info("Token {} RE-QUEUED as #{} for shop {}", tokenId, nextTokenNumber, token.getShop().getId());

        triggerSseBroadcast(token.getShop().getId());

        long waitingAhead = tokenRepository.countBySessionIdAndStatus(session.getId(), TokenStatus.WAITING);
        return queueMapper.toTokenResponseDto(token, (int) Math.max(0, waitingAhead - 1));
    }

    @Override
    @Transactional
    public QueueStatusResponseDto pauseQueue(UUID shopId, in.abhiikhula.queue.dto.PauseQueueRequest request, User currentUser) {
        verifyShopOwnership(shopId, currentUser);

        QueueSession session = queueSessionRepository
                .findByShopIdAndStatusIn(shopId, List.of(SessionStatus.ACTIVE))
                .orElseThrow(() -> new BadRequestException("No active queue session found to pause"));

        session.setStatus(SessionStatus.PAUSED);
        session.setPausedUntil(Instant.now().plusSeconds(request.getPauseMinutes() * 60L));
        session.setPauseReason(request.getReason() != null ? request.getReason() : "Queue paused by merchant");
        queueSessionRepository.save(session);

        log.info("Shop {} queue PAUSED for {} mins: {}", shopId, request.getPauseMinutes(), request.getReason());

        triggerSseBroadcast(shopId);

        return getQueueStatus(shopId);
    }

    @Override
    @Transactional
    public QueueStatusResponseDto resumeQueue(UUID shopId, User currentUser) {
        verifyShopOwnership(shopId, currentUser);

        QueueSession session = queueSessionRepository
                .findByShopIdAndStatusIn(shopId, List.of(SessionStatus.PAUSED))
                .orElseThrow(() -> new BadRequestException("No paused queue session found to resume"));

        session.setStatus(SessionStatus.ACTIVE);
        session.setPausedUntil(null);
        session.setPauseReason(null);
        queueSessionRepository.save(session);

        log.info("Shop {} queue RESUMED to ACTIVE", shopId);

        triggerSseBroadcast(shopId);

        return getQueueStatus(shopId);
    }

    @Override
    @Transactional
    public QueueStatusResponseDto resetQueue(UUID shopId, User currentUser) {
        verifyShopOwnership(shopId, currentUser);

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));

        QueueSession oldSession = queueSessionRepository
                .findByShopIdAndStatusIn(shopId, List.of(SessionStatus.ACTIVE, SessionStatus.PAUSED))
                .orElse(null);

        if (oldSession != null) {
            // Expire all active tokens
            List<Token> pendingTokens = tokenRepository.findBySessionIdAndStatusIn(
                    oldSession.getId(),
                    List.of(TokenStatus.WAITING, TokenStatus.CALLED)
            );
            for (Token t : pendingTokens) {
                t.setStatus(TokenStatus.EXPIRED);
                t.setCancellationReason("Queue reset by merchant");
                tokenRepository.save(t);
                tokenEventRepository.save(TokenEvent.builder()
                        .token(t)
                        .fromStatus(TokenStatus.WAITING)
                        .toStatus(TokenStatus.EXPIRED)
                        .actorUser(currentUser)
                        .note("Session reset by merchant (BRD FR-708)")
                        .build());

                notificationService.sendQueueNotification(
                        t,
                        in.abhiikhula.notification.model.NotificationTemplate.QUEUE_RESET,
                        null
                );
            }

            oldSession.setStatus(SessionStatus.CLOSED);
            oldSession.setClosedAt(Instant.now());
            queueSessionRepository.saveAndFlush(oldSession);
        }

        // Start fresh new session
        QueueSession newSession = QueueSession.builder()
                .shop(shop)
                .sessionDate(LocalDate.now())
                .status(SessionStatus.ACTIVE)
                .currentTokenNumber(0)
                .totalTokensIssued(0)
                .openedAt(Instant.now())
                .build();
        queueSessionRepository.save(newSession);

        log.info("Shop {} queue RESET successfully. New session started (BRD FR-708).", shopId);

        triggerSseBroadcast(shopId);

        return getQueueStatus(shopId);
    }

    private double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_KM = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(rLat1) * Math.cos(rLat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    @Override
    @Transactional(readOnly = true)
    public QueueStatusResponseDto getQueueStatus(UUID shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));

        QueueSession session = queueSessionRepository
                .findByShopIdAndStatusIn(shopId, List.of(SessionStatus.ACTIVE, SessionStatus.PAUSED))
                .orElse(null);

        if (session == null) {
            return QueueStatusResponseDto.builder()
                    .shopId(shop.getId())
                    .shopName(shop.getName())
                    .sessionId(null)
                    .sessionStatus(SessionStatus.CLOSED)
                    .currentServingTokenNumber(0)
                    .currentServingDisplayToken(null)
                    .totalTokensIssued(0)
                    .waitingCount(0)
                    .estimatedWaitMin(0)
                    .acceptingTokens(false)
                    .build();
        }

        long waitingCount = tokenRepository.countBySessionIdAndStatus(session.getId(), TokenStatus.WAITING);
        int serviceDuration = shop.getCategory() != null ? shop.getCategory().getDefaultServiceMin() : 15;
        int delay = session.getDelayMinutes() != null ? session.getDelayMinutes() : 0;
        int estimatedWait = (int) (waitingCount * serviceDuration) + delay;

        String servingDisplay = session.getCurrentTokenNumber() > 0 ? "#" + session.getCurrentTokenNumber() : null;
        boolean accepting = session.getStatus() == SessionStatus.ACTIVE && shop.getStatus() != ShopStatus.CLOSED;

        return queueMapper.toQueueStatusResponseDto(
                session,
                (int) waitingCount,
                estimatedWait,
                accepting,
                servingDisplay
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TokenResponseDto> getMyActiveTokens(User currentUser) {
        List<Token> activeTokens = tokenRepository.findByUserIdAndStatusInOrderByJoinedAtDesc(
                currentUser.getId(),
                List.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.SERVING)
        );
        return queueMapper.toTokenResponseDtoList(activeTokens);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponseDto getTokenById(UUID tokenId, User currentUser) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token", "id", tokenId));

        long waitingAhead = 0;
        if (token.getStatus() == TokenStatus.WAITING) {
            List<Token> waitingTokens = tokenRepository.findBySessionIdAndStatusOrderByTokenNumberAsc(
                    token.getSession().getId(), TokenStatus.WAITING
            );
            waitingAhead = waitingTokens.stream()
                    .filter(t -> t.getTokenNumber() < token.getTokenNumber())
                    .count();
        }

        TokenResponseDto dto = queueMapper.toTokenResponseDto(token, (int) waitingAhead);

        // Privacy protection (BRD FR-1603): Minimal fields in public read.
        // If requester is not the token holder, not the shopkeeper, and not admin -> mask phone
        boolean isTokenOwner = currentUser != null && token.getUser() != null && token.getUser().getId().equals(currentUser.getId());
        boolean isShopOwner = currentUser != null && token.getShop().getOwner() != null && token.getShop().getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.getRole() == UserRole.ADMIN;

        if (!isTokenOwner && !isShopOwner && !isAdmin) {
            dto.setCustomerPhone(maskPhone(token.getCustomerPhone()));
        }

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TokenResponseDto> getGroupTokens(UUID groupId, User currentUser) {
        if (groupId == null) {
            throw new BadRequestException("GroupId is required");
        }
        List<Token> tokens = tokenRepository.findByGroupIdOrderByGroupMemberIndexAsc(groupId);
        if (tokens.isEmpty()) {
            throw new ResourceNotFoundException("GroupTokens", "groupId", groupId);
        }
        return queueMapper.toTokenResponseDtoList(tokens);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return null;
        }
        return "XXXXXX" + phone.substring(phone.length() - 4);
    }

    private void triggerSseBroadcast(UUID shopId) {
        try {
            QueueStatusResponseDto status = getQueueStatus(shopId);
            queueSseBroadcaster.broadcast(shopId, status);
        } catch (Exception e) {
            log.warn("Failed to broadcast SSE update for shop {}: {}", shopId, e.getMessage());
        }
    }

    private void verifyShopOwnership(UUID shopId, User currentUser) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));

        boolean isOwner = shop.getOwner() != null && shop.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You are not authorized to manage the queue for this shop");
        }
    }
}

