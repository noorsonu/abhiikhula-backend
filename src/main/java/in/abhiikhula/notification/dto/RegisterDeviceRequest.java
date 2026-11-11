package in.abhiikhula.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to register a device / FCM token for push notifications")
public class RegisterDeviceRequest {

    @NotBlank(message = "FCM token is required")
    @Schema(description = "FCM or browser push token", example = "fcm_token_xyz_123456")
    private String fcmToken;

    @Schema(description = "Device type: WEB, ANDROID, IOS", example = "WEB")
    private String deviceType;

    @Schema(description = "User agent string", example = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...")
    private String userAgent;
}

