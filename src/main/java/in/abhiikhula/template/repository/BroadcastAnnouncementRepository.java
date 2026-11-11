package in.abhiikhula.template.repository;

import in.abhiikhula.template.model.BroadcastAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BroadcastAnnouncementRepository extends JpaRepository<BroadcastAnnouncement, UUID> {

    Optional<BroadcastAnnouncement> findFirstByCreatedAtAfterOrderByCreatedAtDesc(Instant cutoff);
}

