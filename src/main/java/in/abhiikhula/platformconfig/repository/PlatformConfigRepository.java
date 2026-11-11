package in.abhiikhula.platformconfig.repository;

import in.abhiikhula.platformconfig.model.ConfigScope;
import in.abhiikhula.platformconfig.model.PlatformConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformConfigRepository extends JpaRepository<PlatformConfig, UUID> {

    Optional<PlatformConfig> findByConfigKeyAndScopeAndScopeId(String configKey, ConfigScope scope, UUID scopeId);

    Optional<PlatformConfig> findByConfigKeyAndScope(String configKey, ConfigScope scope);

    List<PlatformConfig> findAllByOrderByConfigKeyAsc();
}

