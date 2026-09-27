package com.moduplaylist.core.user.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.core.user.exception.InvalidOAuthAccountException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(
    name = "user_oauth_accounts",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_oauth_provider_user", columnNames = {"provider", "provider_user_id"}),
        @UniqueConstraint(name = "uq_user_oauth_user_provider", columnNames = {"user_id", "provider"})
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserOAuthAccount {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name= "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, columnDefinition = "ENUM('GOOGLE', 'KAKAO'")
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    private UserOAuthAccount(User user, OAuthProvider provider, String providerUserId) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

    public static UserOAuthAccount create(User user, OAuthProvider provider, String providerUserId) {
        if (user == null || provider == null || providerUserId == null
            || providerUserId.isBlank() || providerUserId.length() > 255) {
            throw new InvalidOAuthAccountException();
        }

        return new UserOAuthAccount(user, provider, providerUserId);
    }

    @PrePersist
    protected void init() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }
}
