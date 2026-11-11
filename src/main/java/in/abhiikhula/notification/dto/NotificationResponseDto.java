package in.abhiikhula.notification.dto;

import in.abhiikhula.notification.model.NotificationChannel;
import in.abhiikhula.notification.model.NotificationTemplate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Customer in-app notification item (BRD FR-401 to FR-405)")
public class NotificationResponseDto {

    private UUID id;
    private UUID shopId;
    private String shopName;
    private UUID tokenId;
    private NotificationTemplate template;
    private NotificationChannel channel;
    private String title;
    private String body;
    private Boolean read;
    private Instant sentAt;
}

