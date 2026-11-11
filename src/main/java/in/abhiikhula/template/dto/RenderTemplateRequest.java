package in.abhiikhula.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dry-run template preview or test send request (BRD FR-1403, FR-1404)")
public class RenderTemplateRequest {

    @NotNull(message = "Template ID is required")
    private UUID templateId;

    @Schema(description = "Placeholder replacement key-values", example = "{\"shop\":\"Royal Barber\",\"number\":\"12\",\"eta\":\"15\"}")
    private Map<String, String> variables;

    @Schema(description = "Optional recipient phone or device for test dry-run send (FR-1404)", example = "9876543210")
    private String testRecipient;
}

