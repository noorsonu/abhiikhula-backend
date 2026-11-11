package in.abhiikhula.billing.service;

import in.abhiikhula.billing.dto.*;
import in.abhiikhula.user.model.User;

import java.util.List;
import java.util.UUID;

public interface BillingService {

    SubscriptionResponseDto getShopSubscription(UUID shopId, User currentUser);

    InvoiceResponseDto requestPlanUpgrade(UUID shopId, UpgradeSubscriptionRequest request, User currentUser);

    List<InvoiceResponseDto> getShopInvoices(UUID shopId, User currentUser);

    boolean canIssueToken(UUID shopId);

    void recordTokenIssued(UUID shopId);

    List<SubscriptionResponseDto> listAllSubscriptions(User admin);

    SubscriptionResponseDto assignPlan(UUID subscriptionId, AssignPlanRequest request, User admin);

    InvoiceResponseDto markInvoicePaid(UUID invoiceId, MarkInvoicePaidRequest request, User admin);

    InvoiceResponseDto refundInvoice(UUID invoiceId, String reason, User admin);
}

