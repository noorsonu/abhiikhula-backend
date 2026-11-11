package in.abhiikhula.claim.repository;

import in.abhiikhula.claim.model.ClaimStatus;
import in.abhiikhula.claim.model.ShopClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShopClaimRepository extends JpaRepository<ShopClaim, UUID> {

    List<ShopClaim> findByApplicantIdOrderByCreatedAtDesc(UUID applicantId);

    List<ShopClaim> findByStatusOrderByCreatedAtDesc(ClaimStatus status);

    List<ShopClaim> findByShopIdOrderByCreatedAtDesc(UUID shopId);

    boolean existsByShopIdAndStatus(UUID shopId, ClaimStatus status);

    boolean existsByShopIdAndApplicantIdAndStatus(UUID shopId, UUID applicantId, ClaimStatus status);
}

