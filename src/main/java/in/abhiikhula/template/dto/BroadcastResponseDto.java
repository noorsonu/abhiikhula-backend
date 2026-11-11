package in.abhiikhula.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Broadcast announcement response")
public class BroadcastResponseDto {

    private UUID id;
    private String title;
    private String body;
    private String targetRole;
    private Integer sentCount;
    private Instant createdAt;
}

