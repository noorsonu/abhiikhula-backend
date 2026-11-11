package in.abhiikhula.template.dto;

import in.abhiikhula.template.model.TemplateChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Notification template response (BRD Section 25, FR-1401)")
public class NotificationTemplateResponseDto {

    private UUID id;
    private String slug;
    private TemplateChannel channel;
    private String locale;
    private String titleTemplate;
    private String bodyTemplate;
    private String variables;
    private Integer version;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}

