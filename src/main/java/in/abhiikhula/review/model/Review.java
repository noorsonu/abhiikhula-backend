package in.abhiikhula.review.model;

import in.abhiikhula.queue.model.Token;
import in.abhiikhula.shop.model.Shop;
import in.abhiikhula.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "token_id", nullable = false, unique = true)
    private Token token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(nullable = false)
    private Integer stars;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(length = 255)
    private String tags;

    @Column(name = "verified_token", nullable = false)
    @Builder.Default
    private Boolean verifiedToken = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hidden = false;

    @Column(name = "abuse_reported", nullable = false)
    @Builder.Default
    private Boolean abuseReported = false;

    @Column(name = "abuse_reason", columnDefinition = "TEXT")
    private String abuseReason;

    @Column(columnDefinition = "TEXT")
    private String reply;

    @Column(name = "reply_at")
    private Instant replyAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

