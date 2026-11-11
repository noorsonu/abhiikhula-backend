package in.abhiikhula.billing.service;

import in.abhiikhula.billing.dto.*;
import in.abhiikhula.billing.mapper.BillingMapper;
import in.abhiikhula.billing.model.*;
import in.abhiikhula.billing.repository.InvoiceRepository;
import in.abhiikhula.billing.repository.SubscriptionRepository;
import in.abhiikhula.common.exception.BadRequestException;
import in.abhiikhula.common.exception.ForbiddenException;
import in.abhiikhula.common.exception.ResourceNotFoundException;
import in.abhiikhula.common.exception.UnauthorizedException;
import in.abhiikhula.platformconfig.service.PlatformConfigService;
import in.abhiikhula.shop.model.Shop;
import in.abhiikhula.shop.repository.ShopRepository;
import in.abhiikhula.user.model.User;
import in.abhiikhula.user.model.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingServiceImpl implements BillingService {

    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final ShopRepository shopRepository;
    private final BillingMapper billingMapper;
    private final PlatformConfigService platformConfigService;

    private static final int FREE_PLAN_DAILY_LIMIT = 15; // BRD FR-905 default
    private static final int UNLIMITED_DAILY_LIMIT = 99999;

    @Override
    @Transactional
    public SubscriptionResponseDto getShopSubscription(UUID shopId, User currentUser) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));
        verifyShopAccess(shop, currentUser);

        Subscription subscription = getOrCreateSubscription(shop);
        checkAndResetDailyTokens(subscription);

        boolean paywallActive = isPaywallActive();
        int dailyLimit = getDailyLimitForPlan(subscription.getPlanCode());

        return billingMapper.toSubscriptionDto(subscription, dailyLimit, paywallActive);
    }

    @Override
    @Transactional
    public InvoiceResponseDto requestPlanUpgrade(UUID shopId, UpgradeSubscriptionRequest request, User currentUser) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));
        verifyShopAccess(shop, currentUser);

        Subscription subscription = getOrCreateSubscription(shop);

        BigDecimal amount;
        switch (request.getTargetPlan()) {
            case LIVE_PRO -> amount = new BigDecimal("399.00");
            case LIVE_PRO_PLUS -> amount = new BigDecimal("699.00");
            default -> throw new BadRequestException("Invalid upgrade plan: " + request.getTargetPlan());
        }

        String invoiceNumber = "INV-" + LocalDate.now().getYear() + "-" + String.format("%05d", invoiceRepository.count() + 1);

        Invoice invoice = Invoice.builder()
                .shop(shop)
                .subscription(subscription)
                .invoiceNumber(invoiceNumber)
                .amount(amount)
                .currency("INR")
                .status(InvoiceStatus.PENDING)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.OFFLINE_UPI)
                .paymentRef(request.getPaymentRef())
                .notes(request.getNotes() != null ? request.getNotes() : "Upgrade to " + request.getTargetPlan())
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Upgrade invoice {} created for shop {} (plan: {}, amount: {})", invoiceNumber, shopId, request.getTargetPlan(), amount);

        return billingMapper.toInvoiceDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponseDto> getShopInvoices(UUID shopId, User currentUser) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));
        verifyShopAccess(shop, currentUser);

        List<Invoice> invoices = invoiceRepository.findByShopIdOrderByCreatedAtDesc(shopId);
        return billingMapper.toInvoiceDtoList(invoices);
    }

    @Override
    @Transactional
    public boolean canIssueToken(UUID shopId) {
        if (!isPaywallActive()) {
            return true; // 100% Free during Phase 1 startup pilot
        }

        Shop shop = shopRepository.findById(shopId).orElse(null);
        if (shop == null) {
            return true;
        }

        Subscription sub = getOrCreateSubscription(shop);
        checkAndResetDailyTokens(sub);

        int limit = getDailyLimitForPlan(sub.getPlanCode());
        return sub.getTokensUsedToday() < limit;
    }

    @Override
    @Transactional
    public void recordTokenIssued(UUID shopId) {
        Shop shop = shopRepository.findById(shopId).orElse(null);
        if (shop == null) return;

        Subscription sub = getOrCreateSubscription(shop);
        checkAndResetDailyTokens(sub);
        sub.setTokensUsedToday(sub.getTokensUsedToday() + 1);
        subscriptionRepository.save(sub);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponseDto> listAllSubscriptions(User admin) {
        validateAdmin(admin);
        boolean paywallActive = isPaywallActive();
        return subscriptionRepository.findAll().stream()
                .map(sub -> billingMapper.toSubscriptionDto(sub, getDailyLimitForPlan(sub.getPlanCode()), paywallActive))
                .toList();
    }

    @Override
    @Transactional
    public SubscriptionResponseDto assignPlan(UUID subscriptionId, AssignPlanRequest request, User admin) {
        validateAdmin(admin);
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        subscription.setPlanCode(request.getPlanCode());
        if (request.getStatus() != null) {
            subscription.setStatus(request.getStatus());
        }
        if (request.getDurationDays() != null && request.getDurationDays() > 0) {
            subscription.setCurrentPeriodEnd(Instant.now().plus(request.getDurationDays(), ChronoUnit.DAYS));
        } else if (request.getPlanCode() == PlanCode.LIFETIME_FREE_PRO) {
            subscription.setCurrentPeriodEnd(null); // Lifetime indefinite
        }

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Admin {} assigned plan {} to subscription {} (shop: {})", admin.getId(), request.getPlanCode(), subscriptionId, saved.getShop().getName());

        return billingMapper.toSubscriptionDto(saved, getDailyLimitForPlan(saved.getPlanCode()), isPaywallActive());
    }

    @Override
    @Transactional
    public InvoiceResponseDto markInvoicePaid(UUID invoiceId, MarkInvoicePaidRequest request, User admin) {
        validateAdmin(admin);
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());
        if (request.getPaymentRef() != null && !request.getPaymentRef().isBlank()) {
            invoice.setPaymentRef(request.getPaymentRef());
        }
        if (request.getNotes() != null) {
            invoice.setNotes(request.getNotes());
        }
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Activate subscription on payment
        Subscription sub = invoice.getSubscription();
        if (sub != null) {
            PlanCode plan = invoice.getAmount().compareTo(new BigDecimal("500.00")) > 0 ? PlanCode.LIVE_PRO_PLUS : PlanCode.LIVE_PRO;
            sub.setPlanCode(plan);
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setCurrentPeriodStart(Instant.now());
            sub.setCurrentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
            subscriptionRepository.save(sub);
            log.info("Subscription for shop {} upgraded to {} upon invoice {} settlement", sub.getShop().getId(), plan, invoice.getInvoiceNumber());
        }

        return billingMapper.toInvoiceDto(savedInvoice);
    }

    @Override
    @Transactional
    public InvoiceResponseDto refundInvoice(UUID invoiceId, String reason, User admin) {
        validateAdmin(admin);
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        invoice.setStatus(InvoiceStatus.REFUNDED);
        invoice.setNotes((invoice.getNotes() != null ? invoice.getNotes() + " | " : "") + "Refunded: " + reason);
        Invoice saved = invoiceRepository.save(invoice);

        return billingMapper.toInvoiceDto(saved);
    }

    private Subscription getOrCreateSubscription(Shop shop) {
        return subscriptionRepository.findByShopId(shop.getId())
                .orElseGet(() -> subscriptionRepository.save(Subscription.builder()
                        .shop(shop)
                        .planCode(PlanCode.LIFETIME_FREE_PRO) // Pilot default
                        .status(SubscriptionStatus.ACTIVE)
                        .currentPeriodStart(Instant.now())
                        .build()));
    }

    private void checkAndResetDailyTokens(Subscription subscription) {
        if (subscription.getLastTokenResetDate().isBefore(LocalDate.now())) {
            subscription.setTokensUsedToday(0);
            subscription.setLastTokenResetDate(LocalDate.now());
            subscriptionRepository.save(subscription);
        }
    }

    private int getDailyLimitForPlan(PlanCode planCode) {
        if (planCode == null) return FREE_PLAN_DAILY_LIMIT;
        return switch (planCode) {
            case FREE -> platformConfigService != null ? platformConfigService.getIntConfig("free_tokens_per_day", FREE_PLAN_DAILY_LIMIT) : FREE_PLAN_DAILY_LIMIT;
            case LIVE_PRO, LIVE_PRO_PLUS, LIFETIME_FREE_PRO -> UNLIMITED_DAILY_LIMIT;
        };
    }

    private boolean isPaywallActive() {
        return platformConfigService != null && platformConfigService.isFlagEnabled("flags.monetization", false);
    }

    private void verifyShopAccess(Shop shop, User currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (currentUser.getRole() == UserRole.ADMIN) {
            return;
        }
        if (shop.getOwner() == null || !shop.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not authorized to access subscription details for this shop");
        }
    }

    private void validateAdmin(User admin) {
        if (admin == null) {
            throw new UnauthorizedException("Admin authentication required");
        }
        if (admin.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Administrative privileges required");
        }
    }
}

