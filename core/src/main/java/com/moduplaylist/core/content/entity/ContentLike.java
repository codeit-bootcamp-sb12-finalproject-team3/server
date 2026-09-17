package com.moduplaylist.core.content.entity;

import com.moduplaylist.core.content.exception.ContentNotLikeableException;
import com.moduplaylist.core.user.entity.User;
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

import java.time.Instant;
import java.util.Objects;

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

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "user_id",
		nullable = false,
		updatable = false
	)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "content_id",
		nullable = false,
		updatable = false
	)
	private Content content;

	@CreationTimestamp
	@Column(
		name = "created_at",
		nullable = false,
		updatable = false
	)
	private Instant createdAt;

	private ContentLike(User user, Content content) {
		this.user = Objects.requireNonNull(user, "user는 필수입니다.");
		this.content = Objects.requireNonNull(content, "content는 필수입니다.");
		if (!content.isLikeable()) {
			throw new ContentNotLikeableException(content.getId());
		}
	}

	public static ContentLike create(User user, Content content) {
		return new ContentLike(user, content);
	}
}
