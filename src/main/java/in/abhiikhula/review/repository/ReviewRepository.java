package in.abhiikhula.review.repository;

import in.abhiikhula.review.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByTokenId(UUID tokenId);

    Optional<Review> findByTokenId(UUID tokenId);

    List<Review> findByShopIdAndHiddenFalseOrderByCreatedAtDesc(UUID shopId);

    List<Review> findByShopIdOrderByCreatedAtDesc(UUID shopId);

    List<Review> findByAbuseReportedTrueOrderByCreatedAtDesc();

    @Query("SELECT AVG(r.stars) FROM Review r WHERE r.shop.id = :shopId AND r.hidden = false")
    Double getAverageStarsByShopId(@Param("shopId") UUID shopId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.shop.id = :shopId AND r.hidden = false")
    long countActiveReviewsByShopId(@Param("shopId") UUID shopId);
}

