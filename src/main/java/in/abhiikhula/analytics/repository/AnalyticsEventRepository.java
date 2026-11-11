package in.abhiikhula.analytics.repository;

import in.abhiikhula.analytics.model.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {

    long countByNameAndCreatedAtBetween(String name, Instant start, Instant end);

    long countByZoneIdAndNameAndCreatedAtBetween(UUID zoneId, String name, Instant start, Instant end);

    long countByShopIdAndNameAndCreatedAtBetween(UUID shopId, String name, Instant start, Instant end);

    List<AnalyticsEvent> findByShopIdAndCreatedAtBetweenOrderByCreatedAtDesc(UUID shopId, Instant start, Instant end);

    List<AnalyticsEvent> findByZoneIdAndCreatedAtBetweenOrderByCreatedAtDesc(UUID zoneId, Instant start, Instant end);

    List<AnalyticsEvent> findByNameInAndCreatedAtBetween(Collection<String> names, Instant start, Instant end);

    @Query("SELECT COUNT(DISTINCT e.user.id) FROM AnalyticsEvent e WHERE e.zone.id = :zoneId AND e.user IS NOT NULL AND e.createdAt BETWEEN :start AND :end")
    long countDistinctUsersByZone(@Param("zoneId") UUID zoneId, @Param("start") Instant start, @Param("end") Instant end);
}

