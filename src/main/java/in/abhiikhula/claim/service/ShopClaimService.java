package in.abhiikhula.claim.service;

import in.abhiikhula.claim.dto.ClaimResponseDto;
import in.abhiikhula.claim.dto.ReviewClaimRequest;
import in.abhiikhula.claim.dto.SubmitClaimRequest;
import in.abhiikhula.claim.model.ClaimStatus;
import in.abhiikhula.user.model.User;

import java.util.List;
import java.util.UUID;

public interface ShopClaimService {

    ClaimResponseDto submitClaim(UUID shopId, SubmitClaimRequest request, User applicant);

    List<ClaimResponseDto> getMyClaims(User applicant);

    List<ClaimResponseDto> getClaimsByStatus(ClaimStatus status, User admin);

    ClaimResponseDto reviewClaim(UUID claimId, ReviewClaimRequest request, User admin);
}

