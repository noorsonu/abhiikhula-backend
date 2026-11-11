package in.abhiikhula.dispute.controller;

import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.dispute.dto.CreateDisputeRequest;
import in.abhiikhula.dispute.dto.DisputeResponseDto;
import in.abhiikhula.dispute.dto.SupportFaqDto;
import in.abhiikhula.dispute.service.DisputeService;
import in.abhiikhula.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "M13 Support & Disputes - Customer", description = "Customer dispute ticket filing and support FAQ (BRD Section 24, FR-1301, FR-1306)")
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping("/api/disputes")
    @Operation(summary = "File dispute ticket", description = "Creates a dispute ticket with shop/token context and auto-attaches last 3 status events as evidence (FR-1301, FR-1302)")
    public ResponseEntity<ApiResponse<DisputeResponseDto>> createDispute(
            @Valid @RequestBody CreateDisputeRequest request,
            @AuthenticationPrincipal User currentUser) {
        DisputeResponseDto dispute = disputeService.createDispute(request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(dispute, "Dispute ticket created successfully"));
    }

    @GetMapping("/api/support/faq")
    @Operation(summary = "Get support FAQs", description = "Returns customer help and frequently asked questions (FR-1306)")
    public ResponseEntity<ApiResponse<List<SupportFaqDto>>> getFaqs() {
        List<SupportFaqDto> faqs = disputeService.getFaqs();
        return ResponseEntity.ok(ApiResponse.ok(faqs, "Support FAQs fetched successfully"));
    }
}

