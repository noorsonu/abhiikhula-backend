package in.abhiikhula.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Platform-wide broadcast announcement (BRD Section 25, FR-1405)")
public class BroadcastRequest {

    @NotBlank(message = "Title is required")
    @Schema(example = "Holi Festival Update")
    private String title;

    @NotBlank(message = "Body is required")
    @Schema(example = "Check live status for festival market timings on AbhiiKhula!")
    private String body;

    @Schema(description = "Target audience: ALL, CUSTOMER, SHOP_OWNER", example = "ALL")
    @Builder.Default
    private String targetRole = "ALL";
}

