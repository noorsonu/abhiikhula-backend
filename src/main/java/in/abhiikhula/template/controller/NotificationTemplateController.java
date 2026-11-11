package in.abhiikhula.template.controller;

import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.template.dto.*;
import in.abhiikhula.template.service.NotificationTemplateService;
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
@RequestMapping("/api/admin/templates")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "M14 Dynamic Comms & Notification Templates Engine", description = "Admin dynamic notification templates, preview/test send, and broadcast announcements (BRD Section 25, FR-1401 to FR-1405)")
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;

    @GetMapping
    @Operation(summary = "List all notification templates", description = "Returns active & inactive notification templates across channels (PUSH/SMS) and locales (hi/en)")
    public ResponseEntity<ApiResponse<List<NotificationTemplateResponseDto>>> listTemplates() {
        List<NotificationTemplateResponseDto> templates = templateService.listTemplates();
        return ResponseEntity.ok(ApiResponse.ok(templates, "Notification templates fetched successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID", description = "Returns single template with placeholder variables")
    public ResponseEntity<ApiResponse<NotificationTemplateResponseDto>> getTemplate(@PathVariable UUID id) {
        NotificationTemplateResponseDto template = templateService.getTemplateById(id);
        return ResponseEntity.ok(ApiResponse.ok(template, "Notification template fetched successfully"));
    }

    @PostMapping
    @Operation(summary = "Upsert notification template", description = "Creates new or bumps version of existing template by (slug, channel, locale) (FR-1402)")
    public ResponseEntity<ApiResponse<NotificationTemplateResponseDto>> upsertTemplate(
            @Valid @RequestBody UpsertTemplateRequest request,
            @AuthenticationPrincipal User admin) {
        NotificationTemplateResponseDto saved = templateService.upsertTemplate(request, admin);
        return ResponseEntity.ok(ApiResponse.ok(saved, "Notification template saved successfully"));
    }

    @PostMapping("/preview")
    @Operation(summary = "Preview or dry-run test send template", description = "Replaces placeholders like {{shop}}, {{number}} and optionally simulates sending to testRecipient (FR-1403, FR-1404)")
    public ResponseEntity<ApiResponse<RenderedTemplateDto>> previewOrTest(
            @Valid @RequestBody RenderTemplateRequest request,
            @AuthenticationPrincipal User admin) {
        RenderedTemplateDto rendered = templateService.previewOrTestTemplate(request, admin);
        return ResponseEntity.ok(ApiResponse.ok(rendered, "Template rendered successfully"));
    }

    @PostMapping("/broadcast")
    @Operation(summary = "Send platform-wide broadcast announcement", description = "Broadcasts system notification to users; strictly rate-limited to max 1 announcement per 7 days (FR-1405)")
    public ResponseEntity<ApiResponse<BroadcastResponseDto>> sendBroadcast(
            @Valid @RequestBody BroadcastRequest request,
            @AuthenticationPrincipal User admin) {
        BroadcastResponseDto broadcast = templateService.sendBroadcast(request, admin);
        return ResponseEntity.ok(ApiResponse.ok(broadcast, "Broadcast announcement sent successfully"));
    }
}

