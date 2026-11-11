package in.abhiikhula.notification.controller;

import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.common.exception.UnauthorizedException;
import in.abhiikhula.notification.dto.NotificationResponseDto;
import in.abhiikhula.notification.dto.RegisterDeviceRequest;
import in.abhiikhula.notification.service.NotificationService;
import in.abhiikhula.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "M4 Notifications & Reminders", description = "In-App Reminders, Device Registration & Alerts (BRD Section 15, FR-401 to FR-410)")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/devices")
    @Operation(summary = "Register Device / FCM Token", description = "Saves browser FCM or device push token for queue alerts (BRD Section 15.1)")
    public ResponseEntity<ApiResponse<Void>> registerDevice(
            @Valid @RequestBody RegisterDeviceRequest request,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        notificationService.registerDevice(request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(null, "Device registered successfully"));
    }

    @GetMapping("/my")
    @Operation(summary = "Get My Notifications", description = "Returns customer's in-app notification history ordered by sentAt descending (BRD FR-401 to FR-405)")
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getMyNotifications(
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        List<NotificationResponseDto> notifications = notificationService.getMyNotifications(currentUser);
        return ResponseEntity.ok(ApiResponse.ok(notifications, "Notifications fetched successfully"));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get Unread Notifications Count", description = "Returns count of unread notifications for badge display on bell icon")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        long count = notificationService.getUnreadCount(currentUser);
        return ResponseEntity.ok(ApiResponse.ok(count, "Unread count fetched"));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark Notification as Read", description = "Updates notification isRead status to true")
    public ResponseEntity<ApiResponse<NotificationResponseDto>> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        NotificationResponseDto notification = notificationService.markAsRead(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(notification, "Notification marked as read"));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark All Notifications as Read", description = "Marks all user's notifications as read in bulk")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        notificationService.markAllAsRead(currentUser);
        return ResponseEntity.ok(ApiResponse.ok(null, "All notifications marked as read"));
    }
}

