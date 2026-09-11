package com.moduplaylist.core.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(
	name = "content_likes",
	indexes = {
		@Index(name = "idx_content_likes_content", columnList = "content_id"),
		@Index(
			name = "idx_content_likes_user_created",
			columnList = "user_id, created_at DESC, content_id"
		)
	},
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uq_content_likes",
			columnNames = {"user_id", "content_id"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentLike extends ContentUuidEntity {

	@Column(
		name = "user_id",
		nullable = false,
		updatable = false,
		columnDefinition = "BINARY(16)"
	)
	private UUID userId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "content_id",
		nullable = false,
		updatable = false
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Content content;

	@CreationTimestamp
	@Column(
		name = "created_at",
		nullable = false,
		updatable = false
	)
	private Instant createdAt;

	private ContentLike(UUID userId, Content content) {
		this.userId = Objects.requireNonNull(userId, "userId는 필수입니다.");
		this.content = Objects.requireNonNull(content, "content는 필수입니다.");
		if (!content.isReviewable()) {
			throw new IllegalArgumentException(
				"TV 시리즈 컨테이너에는 좋아요를 추가할 수 없습니다."
			);
		}
	}

	public static ContentLike create(UUID userId, Content content) {
		return new ContentLike(userId, content);
	}
}
