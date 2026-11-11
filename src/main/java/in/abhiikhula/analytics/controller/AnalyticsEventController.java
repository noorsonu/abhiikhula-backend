package in.abhiikhula.analytics.controller;

import in.abhiikhula.analytics.dto.RecordEventRequest;
import in.abhiikhula.analytics.service.AnalyticsService;
import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/events")
@RequiredArgsConstructor
@Tag(name = "M12 Analytics - Events Ingestion", description = "Consent-gated client event tracking without PII (BRD Section 23, FR-1201, FR-1204)")
public class AnalyticsEventController {

    private final AnalyticsService analyticsService;

    @PostMapping
    @Operation(summary = "Record analytics event", description = "Ingests user engagement events: search, card view, token CTA, check-in without PII")
    public ResponseEntity<ApiResponse<Void>> recordEvent(
            @Valid @RequestBody RecordEventRequest request,
            @AuthenticationPrincipal User currentUser) {
        analyticsService.recordEvent(request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(null, "Event recorded successfully"));
    }
}

