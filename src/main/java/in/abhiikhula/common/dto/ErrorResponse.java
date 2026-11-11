package in.abhiikhula.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized Production Error Response (RFC-7807 aligned)")
public class ErrorResponse {

    @Schema(description = "Success flag, always false for errors", example = "false")
    @Builder.Default
    private boolean success = false;

    @Schema(description = "HTTP Status code", example = "404")
    private int status;

    @Schema(description = "Standard Error Code identifier", example = "RESOURCE_NOT_FOUND")
    private String errorCode;

    @Schema(description = "Descriptive error message", example = "Shop not found with id : 'd1b2c3d4-...'")
    private String message;

    @Schema(description = "Request URI path", example = "/api/shops/123")
    private String path;

    @Schema(description = "Timestamp when error occurred")
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Schema(description = "Field-level validation error details, if any")
    private Map<String, String> errors;
}

