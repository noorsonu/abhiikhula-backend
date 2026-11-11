package in.abhiikhula.notification.service;

import in.abhiikhula.common.exception.ResourceNotFoundException;
import in.abhiikhula.notification.dto.NotificationResponseDto;
import in.abhiikhula.notification.dto.RegisterDeviceRequest;
import in.abhiikhula.notification.mapper.NotificationMapper;
import in.abhiikhula.notification.model.Device;
import in.abhiikhula.notification.model.Notification;
import in.abhiikhula.notification.model.NotificationChannel;
import in.abhiikhula.notification.model.NotificationTemplate;
import in.abhiikhula.notification.repository.DeviceRepository;
import in.abhiikhula.notification.repository.NotificationRepository;
import in.abhiikhula.queue.model.Token;
import in.abhiikhula.user.model.User;
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
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceRepository deviceRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    public void registerDevice(RegisterDeviceRequest request, User currentUser) {
        String token = request.getFcmToken().trim();
        log.info("Registering device token for user: {}", currentUser.getId());

        Device device = deviceRepository.findByUserIdAndFcmToken(currentUser.getId(), token)
                .orElse(null);

        if (device != null) {
            device.setLastSeen(Instant.now());
            if (request.getUserAgent() != null) {
                device.setUserAgent(request.getUserAgent());
            }
            if (request.getDeviceType() != null) {
                device.setDeviceType(request.getDeviceType());
            }
            deviceRepository.save(device);
        } else {
            deviceRepository.save(Device.builder()
                    .user(currentUser)
                    .fcmToken(token)
                    .deviceType(request.getDeviceType() != null ? request.getDeviceType() : "WEB")
                    .userAgent(request.getUserAgent())
                    .lastSeen(Instant.now())
                    .build());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getMyNotifications(User currentUser) {
        List<Notification> list = notificationRepository.findByUserIdOrderBySentAtDesc(currentUser.getId());
        return notificationMapper.toDtoList(list);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(User currentUser) {
        return notificationRepository.countByUserIdAndIsReadFalse(currentUser.getId());
    }

    @Override
    @Transactional
    public NotificationResponseDto markAsRead(UUID notificationId, User currentUser) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        return notificationMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void markAllAsRead(User currentUser) {
        List<Notification> unread = notificationRepository.findByUserIdOrderBySentAtDesc(currentUser.getId())
                .stream()
                .filter(n -> Boolean.FALSE.equals(n.getIsRead()))
                .toList();

        for (Notification n : unread) {
            n.setIsRead(true);
            notificationRepository.save(n);
        }
    }

    @Override
    @Transactional
    public void sendQueueNotification(Token token, NotificationTemplate template, String customNote) {
        if (token == null || token.getUser() == null) {
            log.debug("Token or user is null. Skipping notification dispatch.");
            return;
        }

        String shopName = token.getShop() != null ? token.getShop().getName() : "Shop";
        int tokenNum = token.getTokenNumber() != null ? token.getTokenNumber() : 0;

        String title;
        String body;

        switch (template) {
            case TWO_AHEAD -> {
                title = "Ghar se nikalne ka samay!";
                body = shopName + ": leave in 10 min — token #" + tokenNum + ", two ahead (BRD FR-402).";
            }
            case YOUR_TURN -> {
                title = "Aapka number aa gaya!";
                body = "Your number #" + tokenNum + " at " + shopName + " — come in now (BRD FR-403).";
            }
            case HOLD_EXPIRY -> {
                title = "5 minute bache hain!";
                body = "5 minutes left for token #" + tokenNum + ". If you do not arrive, the token will be skipped (BRD FR-404).";
            }
            case DELAY_ALERT -> {
                title = "Shop Delay Notice";
                body = shopName + ": " + (customNote != null ? customNote : "Queue is running late.");
            }
            case SHOP_CLOSED -> {
                title = "Dukan band ho gayi hai";
                body = shopName + " has closed. Queue will resume tomorrow (BRD FR-405).";
            }
            case QUEUE_RESET -> {
                title = "Queue Reset";
                body = shopName + ": Session reset by merchant. Retake token if needed.";
            }
            default -> {
                title = "AbhiiKhula Alert";
                body = customNote != null ? customNote : ("Queue update for token #" + tokenNum);
            }
        }

        Notification notification = Notification.builder()
                .user(token.getUser())
                .shop(token.getShop())
                .token(token)
                .template(template)
                .channel(NotificationChannel.IN_APP)
                .title(title)
                .body(body)
                .payload(customNote)
                .isRead(false)
                .sentAt(Instant.now())
                .build();

        notificationRepository.save(notification);
        log.info("Sent [{}] notification to user {} for token #{}", template, token.getUser().getId(), tokenNum);
    }
}

