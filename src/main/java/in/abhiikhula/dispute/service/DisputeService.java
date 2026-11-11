package in.abhiikhula.dispute.service;

import in.abhiikhula.dispute.dto.*;
import in.abhiikhula.dispute.model.DisputeState;
import in.abhiikhula.user.model.User;

import java.util.List;
import java.util.UUID;

public interface DisputeService {

    DisputeResponseDto createDispute(CreateDisputeRequest request, User currentUser);

    List<DisputeResponseDto> getShopDisputes(UUID shopId, User owner);

    DisputeResponseDto replyToDispute(UUID shopId, UUID disputeId, MerchantReplyDisputeRequest request, User owner);

    List<DisputeResponseDto> listDisputesForAdmin(DisputeState stateFilter, User admin);

    DisputeResponseDto resolveDispute(UUID disputeId, ResolveDisputeRequest request, User admin);

    List<SupportFaqDto> getFaqs();
}

