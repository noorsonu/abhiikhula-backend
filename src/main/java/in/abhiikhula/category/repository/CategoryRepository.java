package in.abhiikhula.category.repository;

import in.abhiikhula.category.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByActiveTrue();

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    long countByActive(Boolean active);
}

