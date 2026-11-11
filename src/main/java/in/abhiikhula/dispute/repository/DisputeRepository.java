package in.abhiikhula.dispute.repository;

import in.abhiikhula.dispute.model.Dispute;
import in.abhiikhula.dispute.model.DisputeState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    List<Dispute> findByShopIdOrderByCreatedAtDesc(UUID shopId);

    List<Dispute> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Dispute> findByStateOrderByCreatedAtDesc(DisputeState state);

    List<Dispute> findAllByOrderByCreatedAtDesc();

    long countByState(DisputeState state);

    long countByShopIdAndState(UUID shopId, DisputeState state);
}

