package in.abhiikhula.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Consent-gated client analytics event ingestion request (BRD FR-1201, FR-1204)")
public class RecordEventRequest {

    @NotBlank(message = "Event name is required")
    @Schema(description = "Standard event name", example = "SEARCH_OPEN", allowableValues = {"SEARCH_OPEN", "CARD_VIEW", "DETAIL_OPEN", "TOKEN_CTA_TAP", "TOKEN_ISSUED", "CHECKIN", "SERVED", "NO_SHOW", "CANCEL"})
    private String name;

    @Schema(description = "Anonymous client device UUID (no PII)", example = "dev-anon-98124")
    private String deviceId;

    @Schema(description = "Optional related shop UUID")
    private UUID shopId;

    @Schema(description = "Optional related zone UUID")
    private UUID zoneId;

    @Schema(description = "Optional anonymous event metadata without PII (e.g., queryLength, screenWidth)")
    private Map<String, Object> props;
}

