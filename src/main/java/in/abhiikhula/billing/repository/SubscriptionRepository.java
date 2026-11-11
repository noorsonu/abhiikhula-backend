package in.abhiikhula.billing.repository;

import in.abhiikhula.billing.model.PlanCode;
import in.abhiikhula.billing.model.Subscription;
import in.abhiikhula.billing.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByShopId(UUID shopId);

    List<Subscription> findByStatus(SubscriptionStatus status);

    List<Subscription> findByPlanCode(PlanCode planCode);

    boolean existsByShopId(UUID shopId);
}

