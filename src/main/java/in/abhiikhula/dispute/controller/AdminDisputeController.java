package in.abhiikhula.dispute.controller;

import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.dispute.dto.DisputeResponseDto;
import in.abhiikhula.dispute.dto.ResolveDisputeRequest;
import in.abhiikhula.dispute.model.DisputeState;
import in.abhiikhula.dispute.service.DisputeService;
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
@RequestMapping("/api/admin/disputes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "M13 Support & Disputes - Admin Desk", description = "Admin dispute adjudication, warnings, strikes, and credits (BRD Section 24, FR-1305, FR-806, AD-12)")
public class AdminDisputeController {

    private final DisputeService disputeService;

    @GetMapping
    @Operation(summary = "Admin: List platform disputes", description = "Returns disputes filtered by state (NEW, IN_REVIEW, RESOLVED, CLOSED) with evidence")
    public ResponseEntity<ApiResponse<List<DisputeResponseDto>>> listDisputes(
            @RequestParam(required = false) DisputeState state,
            @AuthenticationPrincipal User admin) {
        List<DisputeResponseDto> disputes = disputeService.listDisputesForAdmin(state, admin);
        return ResponseEntity.ok(ApiResponse.ok(disputes, "Platform disputes fetched successfully"));
    }

    @PatchMapping("/{disputeId}/resolve")
    @Operation(summary = "Admin: Resolve dispute ticket", description = "Adjudicates dispute with action (WARN, STRIKE, CREDIT, DISMISS). 3 strikes suspends badge; 5 suspends shop (FR-1305)")
    public ResponseEntity<ApiResponse<DisputeResponseDto>> resolveDispute(
            @PathVariable UUID disputeId,
            @Valid @RequestBody ResolveDisputeRequest request,
            @AuthenticationPrincipal User admin) {
        DisputeResponseDto resolved = disputeService.resolveDispute(disputeId, request, admin);
        return ResponseEntity.ok(ApiResponse.ok(resolved, "Dispute resolved successfully"));
    }
}

