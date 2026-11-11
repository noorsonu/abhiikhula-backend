package in.abhiikhula.shop.repository;

import in.abhiikhula.shop.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopRepository extends JpaRepository<Shop, UUID> {

    @Query("""
            SELECT s FROM Shop s
            JOIN FETCH s.zone z
            JOIN FETCH s.category c
            WHERE (:zoneId IS NULL OR z.id = :zoneId)
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (:openOnly IS NULL OR :openOnly = false OR s.status != 'CLOSED')
              AND (:q IS NULL OR :q = '' 
                   OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) 
                   OR LOWER(s.address) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))
              AND s.active = true
            ORDER BY
              CASE s.status
                WHEN 'OPEN'   THEN 1
                WHEN 'BUSY'   THEN 2
                WHEN 'PAUSED' THEN 3
                ELSE               4
              END,
              s.crowdLevel ASC
            """)
    List<Shop> searchShops(@Param("zoneId") UUID zoneId,
                           @Param("categoryId") UUID categoryId,
                           @Param("openOnly") Boolean openOnly,
                           @Param("q") String q);

    @Query("""
            SELECT s FROM Shop s
            JOIN FETCH s.zone
            JOIN FETCH s.category
            WHERE s.id = :id
            """)
    Optional<Shop> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
            SELECT s FROM Shop s
            JOIN FETCH s.zone
            JOIN FETCH s.category
            WHERE s.slug = :slug
            """)
    Optional<Shop> findBySlugWithDetails(@Param("slug") String slug);

    boolean existsBySlug(String slug);

    List<Shop> findByOwnerId(UUID ownerId);

    List<Shop> findByZoneId(UUID zoneId);

    long countByVerified(Boolean verified);

    long countByActive(Boolean active);
}
