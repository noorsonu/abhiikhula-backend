package in.abhiikhula.queue.repository;

import in.abhiikhula.queue.model.Token;
import in.abhiikhula.queue.model.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {

    Optional<Token> findBySessionIdAndTokenNumber(UUID sessionId, Integer tokenNumber);

    List<Token> findBySessionIdAndStatusOrderByTokenNumberAsc(UUID sessionId, TokenStatus status);

    List<Token> findBySessionIdOrderByTokenNumberAsc(UUID sessionId);

    List<Token> findBySessionIdAndStatusIn(UUID sessionId, Collection<TokenStatus> statuses);

    long countBySessionIdAndStatus(UUID sessionId, TokenStatus status);

    Optional<Token> findFirstBySessionIdAndStatusOrderByTokenNumberAsc(UUID sessionId, TokenStatus status);

    boolean existsByShopIdAndUserIdAndStatusIn(UUID shopId, UUID userId, Collection<TokenStatus> statuses);

    long countByUserIdAndStatusIn(UUID userId, Collection<TokenStatus> statuses);

    List<Token> findByUserIdAndStatusInOrderByJoinedAtDesc(UUID userId, Collection<TokenStatus> statuses);

    List<Token> findByShopIdAndJoinedAtBetween(UUID shopId, java.time.Instant start, java.time.Instant end);

    long countByJoinedAtBetween(java.time.Instant start, java.time.Instant end);

    List<Token> findByGroupIdOrderByGroupMemberIndexAsc(UUID groupId);
}

