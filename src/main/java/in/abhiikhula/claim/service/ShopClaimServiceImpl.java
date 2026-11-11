package in.abhiikhula.claim.service;

import in.abhiikhula.claim.dto.ClaimResponseDto;
import in.abhiikhula.claim.dto.ReviewClaimRequest;
import in.abhiikhula.claim.dto.SubmitClaimRequest;
import in.abhiikhula.claim.mapper.ClaimMapper;
import in.abhiikhula.claim.model.ClaimStatus;
import in.abhiikhula.claim.model.ShopClaim;
import in.abhiikhula.claim.repository.ShopClaimRepository;
import in.abhiikhula.common.exception.BadRequestException;
import in.abhiikhula.common.exception.DuplicateResourceException;
import in.abhiikhula.common.exception.ForbiddenException;
import in.abhiikhula.common.exception.ResourceNotFoundException;
import in.abhiikhula.shop.model.Shop;
import in.abhiikhula.shop.repository.ShopRepository;
import in.abhiikhula.user.model.User;
import in.abhiikhula.user.model.UserRole;
import in.abhiikhula.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopClaimServiceImpl implements ShopClaimService {

    private final ShopClaimRepository shopClaimRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ClaimMapper claimMapper;

    @Override
    @Transactional
    public ClaimResponseDto submitClaim(UUID shopId, SubmitClaimRequest request, User applicant) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));

        if (Boolean.TRUE.equals(shop.getVerified()) && shop.getOwner() != null) {
            throw new DuplicateResourceException("This shop listing is already claimed and verified by its owner.");
        }

        if (shopClaimRepository.existsByShopIdAndApplicantIdAndStatus(shopId, applicant.getId(), ClaimStatus.PENDING)) {
            throw new DuplicateResourceException("You already have an active pending claim for this shop.");
        }

        ShopClaim claim = ShopClaim.builder()
                .shop(shop)
                .applicant(applicant)
                .documentType(request.getDocumentType())
                .documentUrl(request.getDocumentUrl().trim())
                .applicantPhone(request.getApplicantPhone().trim())
                .status(ClaimStatus.PENDING)
                .build();

        ShopClaim savedClaim = shopClaimRepository.save(claim);
        log.info("Claim {} submitted by user {} for shop {}", savedClaim.getId(), applicant.getId(), shopId);

        return claimMapper.toDto(savedClaim);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimResponseDto> getMyClaims(User applicant) {
        List<ShopClaim> claims = shopClaimRepository.findByApplicantIdOrderByCreatedAtDesc(applicant.getId());
        return claimMapper.toDtoList(claims);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimResponseDto> getClaimsByStatus(ClaimStatus status, User admin) {
        verifyAdminRole(admin);

        ClaimStatus searchStatus = (status != null) ? status : ClaimStatus.PENDING;
        List<ShopClaim> claims = shopClaimRepository.findByStatusOrderByCreatedAtDesc(searchStatus);
        return claimMapper.toDtoList(claims);
    }

    @Override
    @Transactional
    public ClaimResponseDto reviewClaim(UUID claimId, ReviewClaimRequest request, User admin) {
        verifyAdminRole(admin);

        ShopClaim claim = shopClaimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("ShopClaim", "id", claimId));

        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new BadRequestException("This claim has already been reviewed (status: " + claim.getStatus() + ")");
        }

        if (request.getStatus() == ClaimStatus.PENDING) {
            throw new BadRequestException("Review decision must be either APPROVED or REJECTED");
        }

        Instant now = Instant.now();
        claim.setReviewedBy(admin);
        claim.setReviewedAt(now);
        claim.setAdminNotes(request.getAdminNotes());

        if (request.getStatus() == ClaimStatus.REJECTED) {
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new BadRequestException("Rejection reason is required when rejecting a claim.");
            }
            claim.setStatus(ClaimStatus.REJECTED);
            claim.setRejectionReason(request.getRejectionReason().trim());
            log.info("Claim {} REJECTED by admin {}. Reason: {}", claimId, admin.getId(), request.getRejectionReason());
        } else if (request.getStatus() == ClaimStatus.APPROVED) {
            claim.setStatus(ClaimStatus.APPROVED);

            // 1. Link shop ownership & mark verified
            Shop shop = claim.getShop();
            shop.setOwner(claim.getApplicant());
            shop.setVerified(true);
            shopRepository.save(shop);

            // 2. Upgrade user role if currently CUSTOMER
            User applicant = claim.getApplicant();
            if (applicant.getRole() == UserRole.CUSTOMER) {
                applicant.setRole(UserRole.SHOP_OWNER);
                userRepository.save(applicant);
                log.info("User {} role upgraded to SHOP_OWNER upon claim approval", applicant.getId());
            }

            // 3. Reject any other competing pending claims for the same shop
            List<ShopClaim> otherPendingClaims = shopClaimRepository.findByShopIdOrderByCreatedAtDesc(shop.getId());
            for (ShopClaim other : otherPendingClaims) {
                if (other.getStatus() == ClaimStatus.PENDING && !other.getId().equals(claim.getId())) {
                    other.setStatus(ClaimStatus.REJECTED);
                    other.setRejectionReason("Shop was claimed and verified by another approved applicant.");
                    other.setReviewedBy(admin);
                    other.setReviewedAt(now);
                    shopClaimRepository.save(other);
                    log.info("Competing claim {} rejected due to claim {} approval", other.getId(), claimId);
                }
            }

            log.info("Claim {} APPROVED by admin {}. Shop {} now owned by user {}", claimId, admin.getId(), shop.getId(), applicant.getId());
        }

        ShopClaim updatedClaim = shopClaimRepository.save(claim);
        return claimMapper.toDto(updatedClaim);
    }

    private void verifyAdminRole(User user) {
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Only administrators can perform this action.");
        }
    }
}

