package in.abhiikhula.ads.repository;

import in.abhiikhula.ads.model.FeaturedSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeaturedSlotRepository extends JpaRepository<FeaturedSlot, UUID> {

    List<FeaturedSlot> findByZoneIdAndCategoryIdAndSlotDateAndActiveTrueOrderByPositionAsc(
            UUID zoneId, UUID categoryId, LocalDate slotDate
    );

    List<FeaturedSlot> findByZoneIdAndSlotDateAndActiveTrueOrderByPositionAsc(
            UUID zoneId, LocalDate slotDate
    );

    List<FeaturedSlot> findByShopIdOrderBySlotDateDesc(UUID shopId);

    Optional<FeaturedSlot> findByZoneIdAndCategoryIdAndSlotDateAndPosition(
            UUID zoneId, UUID categoryId, LocalDate slotDate, Integer position
    );

    boolean existsByZoneIdAndSlotDateAndShopId(UUID zoneId, LocalDate slotDate, UUID shopId);

    List<FeaturedSlot> findBySlotDateBetweenOrderBySlotDateDesc(LocalDate start, LocalDate end);
}

