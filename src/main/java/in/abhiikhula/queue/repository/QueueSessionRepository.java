package in.abhiikhula.queue.repository;

import in.abhiikhula.queue.model.QueueSession;
import in.abhiikhula.queue.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueSessionRepository extends JpaRepository<QueueSession, UUID> {

    Optional<QueueSession> findByShopIdAndStatusIn(UUID shopId, Collection<SessionStatus> statuses);

    Optional<QueueSession> findFirstByShopIdAndStatus(UUID shopId, SessionStatus status);

    Optional<QueueSession> findFirstByShopIdOrderByOpenedAtDesc(UUID shopId);

    long countByStatusIn(Collection<SessionStatus> statuses);
}

