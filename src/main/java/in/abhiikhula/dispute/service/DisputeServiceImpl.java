package in.abhiikhula.dispute.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.common.exception.BadRequestException;
import in.abhiikhula.common.exception.ForbiddenException;
import in.abhiikhula.common.exception.ResourceNotFoundException;
import in.abhiikhula.dispute.dto.*;
import in.abhiikhula.dispute.mapper.DisputeMapper;
import in.abhiikhula.dispute.model.Dispute;
import in.abhiikhula.dispute.model.DisputeState;
import in.abhiikhula.dispute.model.ResolutionAction;
import in.abhiikhula.dispute.repository.DisputeRepository;
import in.abhiikhula.queue.model.Token;
import in.abhiikhula.queue.repository.TokenRepository;
import in.abhiikhula.shop.model.Shop;
import in.abhiikhula.shop.model.ShopStatus;
import in.abhiikhula.shop.model.ShopStatusEvent;
import in.abhiikhula.shop.repository.ShopRepository;
import in.abhiikhula.shop.repository.ShopStatusEventRepository;
import in.abhiikhula.user.model.User;
import in.abhiikhula.user.model.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeServiceImpl implements DisputeService {

    private final DisputeRepository disputeRepository;
    private final ShopRepository shopRepository;
    private final TokenRepository tokenRepository;
    private final ShopStatusEventRepository shopStatusEventRepository;
    private final DisputeMapper disputeMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public DisputeResponseDto createDispute(CreateDisputeRequest request, User currentUser) {
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", request.getShopId()));

        Token token = null;
        if (request.getTokenId() != null) {
            token = tokenRepository.findById(request.getTokenId())
                    .orElseThrow(() -> new ResourceNotFoundException("Token", "id", request.getTokenId()));
            if (!token.getShop().getId().equals(shop.getId())) {
                throw new BadRequestException("Token does not belong to the reported shop");
            }
        }

        // FR-1302: Auto-attach the last three status events and the token as evidence
        String evidenceJson = compileEvidence(shop, token);

        String contactPhone = currentUser != null ? currentUser.getPhone() : request.getCustomerPhone();

        Dispute dispute = Dispute.builder()
                .shop(shop)
                .token(token)
                .user(currentUser)
                .customerPhone(contactPhone)
                .reason(request.getReason())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .evidenceJson(evidenceJson)
                .state(DisputeState.NEW)
                .build();

        Dispute saved = disputeRepository.save(dispute);
        log.warn("Dispute {} created for shop {} with reason {}", saved.getId(), shop.getName(), request.getReason());
        return disputeMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisputeResponseDto> getShopDisputes(UUID shopId, User owner) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));

        verifyShopOwner(shop, owner);

        List<Dispute> disputes = disputeRepository.findByShopIdOrderByCreatedAtDesc(shopId);
        return disputeMapper.toDtoList(disputes);
    }

    @Override
    @Transactional
    public DisputeResponseDto replyToDispute(UUID shopId, UUID disputeId, MerchantReplyDisputeRequest request, User owner) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));

        verifyShopOwner(shop, owner);

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));

        if (!dispute.getShop().getId().equals(shopId)) {
            throw new BadRequestException("Dispute ticket does not belong to this shop");
        }

        dispute.setMerchantReply(request.getReply().trim());
        dispute.setMerchantReplyAt(Instant.now());

        // FR-1303: Transition to IN_REVIEW once merchant responds
        if (dispute.getState() == DisputeState.NEW) {
            dispute.setState(DisputeState.IN_REVIEW);
        }

        Dispute saved = disputeRepository.save(dispute);
        log.info("Merchant replied to dispute {} for shop {}", disputeId, shopId);
        return disputeMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisputeResponseDto> listDisputesForAdmin(DisputeState stateFilter, User admin) {
        validateAdmin(admin);

        List<Dispute> disputes = stateFilter != null ?
                disputeRepository.findByStateOrderByCreatedAtDesc(stateFilter) :
                disputeRepository.findAllByOrderByCreatedAtDesc();

        return disputeMapper.toDtoList(disputes);
    }

    @Override
    @Transactional
    public DisputeResponseDto resolveDispute(UUID disputeId, ResolveDisputeRequest request, User admin) {
        validateAdmin(admin);

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));

        dispute.setResolutionAction(request.getAction());
        dispute.setResolutionNotes(request.getNotes() != null ? request.getNotes().trim() : null);
        dispute.setResolvedBy(admin);
        dispute.setResolvedAt(Instant.now());
        dispute.setState(DisputeState.RESOLVED);

        // FR-1305 & Section 22 Trust Rules:
        // 3 strikes suspends verified badge; 5 strikes suspends the shop listing
        if (request.getAction() == ResolutionAction.STRIKE) {
            Shop shop = dispute.getShop();
            int newStrikes = (shop.getStrikes() != null ? shop.getStrikes() : 0) + 1;
            shop.setStrikes(newStrikes);
            log.warn("STRIKE {} issued against shop {}", newStrikes, shop.getName());

            if (newStrikes >= 3 && Boolean.TRUE.equals(shop.getVerified())) {
                shop.setVerified(false);
                log.warn("Shop {} verified badge suspended due to 3 strikes (BRD Section 22)", shop.getName());
            }

            if (newStrikes >= 5 && Boolean.TRUE.equals(shop.getActive())) {
                shop.setActive(false);
                shop.setStatus(ShopStatus.CLOSED);
                log.error("Shop {} suspended permanently due to 5 strikes (BRD Section 22)", shop.getName());
            }

            shopRepository.save(shop);
        }

        Dispute saved = disputeRepository.save(dispute);
        log.info("Dispute {} resolved by admin {} with action {}", disputeId, admin.getPhone(), request.getAction());
        return disputeMapper.toDto(saved);
    }

    @Override
    public List<SupportFaqDto> getFaqs() {
        return List.of(
                SupportFaqDto.builder()
                        .category("QUEUE")
                        .question("Token kaise kaam karta hai?")
                        .answer("Aap ghar baithe dukan ka live token le sakte hain. Jaise hi 2 log bachenge, aapko phone par aane ka alert mil jayega.")
                        .build(),
                SupportFaqDto.builder()
                        .category("DISPUTE")
                        .question("Agar dukan band nikli toh kya karein?")
                        .answer("App me Report button dabayein aur 'WAS_CLOSED' select karein. System dukan ke live status history ko check karke turant action lega.")
                        .build(),
                SupportFaqDto.builder()
                        .category("HOLD_WINDOW")
                        .question("Hold Window kya hai?")
                        .answer("Jab aapka number call hota hai, dukan par pahunchne ke liye 10 minute ka hold window milta hai.")
                        .build()
        );
    }

    private String compileEvidence(Shop shop, Token token) {
        Map<String, Object> evidence = new LinkedHashMap<>();

        // 1. Token context if present
        if (token != null) {
            Map<String, Object> tokenInfo = new LinkedHashMap<>();
            tokenInfo.put("tokenId", token.getId());
            tokenInfo.put("displayToken", token.getDisplayToken());
            tokenInfo.put("status", token.getStatus());
            tokenInfo.put("joinedAt", token.getJoinedAt());
            tokenInfo.put("estimatedWaitMin", token.getEstimatedWaitMin());
            evidence.put("tokenContext", tokenInfo);
        }

        // 2. Last 3 shop status events (FR-1302)
        List<ShopStatusEvent> events = shopStatusEventRepository.findTop10ByShopIdOrderByCreatedAtDesc(shop.getId())
                .stream()
                .limit(3)
                .toList();

        List<Map<String, Object>> eventList = new ArrayList<>();
        for (ShopStatusEvent e : events) {
            Map<String, Object> em = new LinkedHashMap<>();
            em.put("fromState", e.getFromStatus());
            em.put("toState", e.getToStatus());
            em.put("crowd", e.getCrowdLevel());
            em.put("source", e.getSource());
            em.put("timestamp", e.getCreatedAt());
            em.put("note", e.getNote());
            eventList.add(em);
        }
        evidence.put("lastStatusEvents", eventList);

        try {
            return objectMapper.writeValueAsString(evidence);
        } catch (Exception e) {
            log.warn("Failed to serialize dispute evidence: {}", e.getMessage());
            return "{}";
        }
    }

    private void verifyShopOwner(Shop shop, User user) {
        if (user == null) {
            throw new ForbiddenException("Authentication required");
        }
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (shop.getOwner() == null || !shop.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("You are not authorized to access disputes for this shop");
        }
    }

    private void validateAdmin(User admin) {
        if (admin == null || admin.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Admin privileges required to access this resource");
        }
    }
}

