package com.moduplaylist.core.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Getter
@Entity
@Table(
	name = "content_tags",
	indexes = {
		@Index(
			name = "idx_content_tags_content_source",
			columnList = "content_id, source"
		),
		@Index(name = "idx_content_tags_tag", columnList = "tag_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uq_content_tags",
			columnNames = {"content_id", "tag_id"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentTag extends ContentUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "content_id",
		nullable = false,
		updatable = false
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Content content;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "tag_id",
		nullable = false,
		updatable = false
	)
	private Tag tag;

	@Enumerated(EnumType.STRING)
	@Column(
		name = "source",
		nullable = false,
		updatable = false,
		length = 10
	)
	private TagSource source;

	private ContentTag(
		Content content,
		Tag tag,
		TagSource source
	) {
		this.content = Objects.requireNonNull(content, "content는 필수입니다.");
		this.tag = Objects.requireNonNull(tag, "tag는 필수입니다.");
		this.source = Objects.requireNonNull(source, "source는 필수입니다.");
		if (source == TagSource.AI && !content.isReviewable()) {
			throw new IllegalArgumentException(
				"TV 시리즈 컨테이너에는 AI 태그를 추가할 수 없습니다."
			);
		}
	}

	public static ContentTag create(
		Content content,
		Tag tag,
		TagSource source
	) {
		return new ContentTag(content, tag, source);
	}
}
