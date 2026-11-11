package in.abhiikhula.queue.model;

import in.abhiikhula.shop.model.Shop;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "queue_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(name = "session_date", nullable = false)
    @Builder.Default
    private LocalDate sessionDate = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "current_token_number", nullable = false)
    @Builder.Default
    private Integer currentTokenNumber = 0;

    @Column(name = "total_tokens_issued", nullable = false)
    @Builder.Default
    private Integer totalTokensIssued = 0;

    @Column(name = "opened_at", nullable = false)
    @Builder.Default
    private Instant openedAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "delay_minutes", nullable = false)
    @Builder.Default
    private Integer delayMinutes = 0;

    @Column(name = "delay_reason")
    private String delayReason;

    @Column(name = "paused_until")
    private Instant pausedUntil;

    @Column(name = "pause_reason")
    private String pauseReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

