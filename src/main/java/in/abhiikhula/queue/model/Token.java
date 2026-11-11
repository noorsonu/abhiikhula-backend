package in.abhiikhula.queue.model;

import in.abhiikhula.shop.model.Shop;
import in.abhiikhula.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private QueueSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "token_number", nullable = false)
    private Integer tokenNumber;

    @Column(name = "display_token", nullable = false, length = 20)
    private String displayToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TokenStatus status = TokenStatus.WAITING;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String source = "APP";

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "estimated_wait_min", nullable = false)
    @Builder.Default
    private Integer estimatedWaitMin = 15;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "called_at")
    private Instant calledAt;

    @Column(name = "served_at")
    private Instant servedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "checked_in", nullable = false)
    @Builder.Default
    private Boolean checkedIn = false;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "checkin_lat", precision = 9, scale = 6)
    private java.math.BigDecimal checkinLat;

    @Column(name = "checkin_lng", precision = 9, scale = 6)
    private java.math.BigDecimal checkinLng;

    @Column(name = "requeued", nullable = false)
    @Builder.Default
    private Boolean requeued = false;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "group_size", nullable = false)
    @Builder.Default
    private Integer groupSize = 1;

    @Column(name = "group_member_index", nullable = false)
    @Builder.Default
    private Integer groupMemberIndex = 1;
}

