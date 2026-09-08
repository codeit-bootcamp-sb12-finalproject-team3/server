package com.moduplaylist.core.review;

import com.moduplaylist.core.common.BaseEntity;
import com.moduplaylist.core.content.Content;
import com.moduplaylist.core.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uq_reviews_user_content", columnNames = {"user_id", "content_id"})
}, indexes = {
        @Index(name = "idx_reviews_content_created", columnList = "content_id, created_at DESC, id DESC"),
        @Index(name = "idx_reviews_user_created", columnList = "user_id, created_at DESC, id DESC")
})
@Check(name = "chk_reviews_rating",
        constraints = "rating BETWEEN 0.0 AND 5.0 AND MOD(rating * 10, 5) = 0")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {
    private static final BigDecimal MAX_RATING = new BigDecimal("5.0");
    private static final BigDecimal RATING_STEP = new BigDecimal("0.5");

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Content content;

    // 리뷰 대상 Content와 구분하기 위해 본문은 reviewText로 명명한다.
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String reviewText;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "is_spoiler", nullable = false)
    private boolean spoiler;

    @PrePersist
    @PreUpdate
    void validate() {
        if (user == null || content == null) {
            throw new IllegalArgumentException("리뷰 작성자와 대상 콘텐츠는 필수입니다.");
        }
        if (reviewText == null || reviewText.isBlank()) {
            throw new IllegalArgumentException("리뷰 내용은 필수입니다.");
        }
        if (rating == null || rating.signum() < 0 || rating.compareTo(MAX_RATING) > 0
                || rating.remainder(RATING_STEP).signum() != 0) {
            throw new IllegalArgumentException("평점은 0부터 5까지 0.5점 단위로 지정해야 합니다.");
        }
    }

    public void update(String reviewText, BigDecimal rating, Boolean spoiler) {
        if (reviewText != null) this.reviewText = reviewText;
        if (rating != null) this.rating = rating;
        if (spoiler != null) this.spoiler = spoiler;
        validate();
    }
}
