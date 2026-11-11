package in.abhiikhula.billing.controller;

import in.abhiikhula.billing.dto.AssignPlanRequest;
import in.abhiikhula.billing.dto.InvoiceResponseDto;
import in.abhiikhula.billing.dto.MarkInvoicePaidRequest;
import in.abhiikhula.billing.dto.SubscriptionResponseDto;
import in.abhiikhula.billing.service.BillingService;
import in.abhiikhula.common.dto.ApiResponse;
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
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "M9 Subscriptions & Billing - Admin", description = "Admin Subscription Management, Offline UPI Verification & Overrides (BRD Section 20, FR-901, FR-909)")
public class AdminBillingController {

    private final BillingService billingService;

    @GetMapping("/subscriptions")
    @Operation(summary = "Admin: List all subscriptions", description = "Returns all active, trial, and suspended shop subscriptions across platform")
    public ResponseEntity<ApiResponse<List<SubscriptionResponseDto>>> listSubscriptions(
            @AuthenticationPrincipal User admin) {
        List<SubscriptionResponseDto> list = billingService.listAllSubscriptions(admin);
        return ResponseEntity.ok(ApiResponse.ok(list, "Subscriptions fetched successfully"));
    }

    @PutMapping("/subscriptions/{subscriptionId}/plan")
    @Operation(summary = "Admin: Assign or override plan", description = "Admin grants plan tier (e.g. LIFETIME_FREE_PRO for pilot partner) or extends validity (BRD FR-901)")
    public ResponseEntity<ApiResponse<SubscriptionResponseDto>> assignPlan(
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody AssignPlanRequest request,
            @AuthenticationPrincipal User admin) {
        SubscriptionResponseDto updated = billingService.assignPlan(subscriptionId, request, admin);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Subscription plan updated successfully"));
    }

    @PatchMapping("/invoices/{invoiceId}/mark-paid")
    @Operation(summary = "Admin: Mark offline UPI payment as PAID", description = "Verifies UTR reference and automatically activates/extends shop subscription (BRD FR-909)")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> markInvoicePaid(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody(required = false) MarkInvoicePaidRequest request,
            @AuthenticationPrincipal User admin) {
        MarkInvoicePaidRequest req = request != null ? request : new MarkInvoicePaidRequest();
        InvoiceResponseDto settled = billingService.markInvoicePaid(invoiceId, req, admin);
        return ResponseEntity.ok(ApiResponse.ok(settled, "Invoice marked as PAID and subscription activated"));
    }

    @PostMapping("/invoices/{invoiceId}/refund")
    @Operation(summary = "Admin: Process and audit invoice refund", description = "Records invoice refund reason for compliance (BRD FR-910)")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> refundInvoice(
            @PathVariable UUID invoiceId,
            @RequestParam(defaultValue = "Customer requested") String reason,
            @AuthenticationPrincipal User admin) {
        InvoiceResponseDto refunded = billingService.refundInvoice(invoiceId, reason, admin);
        return ResponseEntity.ok(ApiResponse.ok(refunded, "Invoice marked as REFUNDED"));
    }
}

