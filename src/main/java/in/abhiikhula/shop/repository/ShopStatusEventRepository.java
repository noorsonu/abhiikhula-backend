package in.abhiikhula.shop.repository;

import in.abhiikhula.shop.model.ShopStatusEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopStatusEventRepository extends JpaRepository<ShopStatusEvent, UUID> {

    List<ShopStatusEvent> findTop10ByShopIdOrderByCreatedAtDesc(UUID shopId);

    Optional<ShopStatusEvent> findFirstByShopIdOrderByCreatedAtDesc(UUID shopId);

    List<ShopStatusEvent> findByShopIdAndCreatedAtBetweenOrderByCreatedAtAsc(UUID shopId, java.time.Instant start, java.time.Instant end);
}

