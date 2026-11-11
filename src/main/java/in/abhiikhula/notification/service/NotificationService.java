package in.abhiikhula.notification.service;

import in.abhiikhula.notification.dto.NotificationResponseDto;
import in.abhiikhula.notification.dto.RegisterDeviceRequest;
import in.abhiikhula.notification.model.NotificationTemplate;
import in.abhiikhula.queue.model.Token;
import in.abhiikhula.user.model.User;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    void registerDevice(RegisterDeviceRequest request, User currentUser);

    List<NotificationResponseDto> getMyNotifications(User currentUser);

    long getUnreadCount(User currentUser);

    NotificationResponseDto markAsRead(UUID notificationId, User currentUser);

    void markAllAsRead(User currentUser);

    void sendQueueNotification(Token token, NotificationTemplate template, String customNote);
}

