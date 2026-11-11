package in.abhiikhula.billing.controller;

import in.abhiikhula.billing.dto.InvoiceResponseDto;
import in.abhiikhula.billing.dto.SubscriptionResponseDto;
import in.abhiikhula.billing.dto.UpgradeSubscriptionRequest;
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
@RequestMapping("/api/shops/{shopId}")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "M9 Subscriptions & Billing - Merchant", description = "Shop Subscription status, upgrades, and invoice history (BRD Section 20)")
public class ShopSubscriptionController {

    private final BillingService billingService;

    @GetMapping("/subscription")
    @Operation(summary = "Get shop subscription details & quota", description = "Fetches active plan, daily token limit, and paywall status for the shop")
    public ResponseEntity<ApiResponse<SubscriptionResponseDto>> getSubscription(
            @PathVariable UUID shopId,
            @AuthenticationPrincipal User currentUser) {
        SubscriptionResponseDto subscription = billingService.getShopSubscription(shopId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(subscription, "Subscription details fetched successfully"));
    }

    @PostMapping("/subscription/upgrade")
    @Operation(summary = "Request plan upgrade (Offline UPI / Pro)", description = "Generates pending invoice with UPI settlement details (BRD FR-901, FR-909)")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> requestUpgrade(
            @PathVariable UUID shopId,
            @Valid @RequestBody UpgradeSubscriptionRequest request,
            @AuthenticationPrincipal User currentUser) {
        InvoiceResponseDto invoice = billingService.requestPlanUpgrade(shopId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(invoice, "Upgrade invoice created. Please settle via UPI."));
    }

    @GetMapping("/invoices")
    @Operation(summary = "Get shop invoice history", description = "Returns historical invoices and payment receipts for this shop (BRD FR-906)")
    public ResponseEntity<ApiResponse<List<InvoiceResponseDto>>> getInvoices(
            @PathVariable UUID shopId,
            @AuthenticationPrincipal User currentUser) {
        List<InvoiceResponseDto> invoices = billingService.getShopInvoices(shopId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(invoices, "Invoices fetched successfully"));
    }
}

