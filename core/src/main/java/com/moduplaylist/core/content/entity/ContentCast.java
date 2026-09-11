package com.moduplaylist.core.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
	name = "content_casts",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uq_content_casts_order",
			columnNames = {"content_id", "display_order"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentCast extends ContentUuidEntity {

	private static final int MAX_NAME_LENGTH = 100;
	private static final int MAX_ROLE_NAME_LENGTH = 255;
	private static final int MAX_PROFILE_IMAGE_URL_LENGTH = 500;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "content_id",
		nullable = false,
		updatable = false
	)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Content content;

	@Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
	private String name;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "role_name", length = MAX_ROLE_NAME_LENGTH)
	private String roleName;

	@Column(name = "profile_image_url", length = MAX_PROFILE_IMAGE_URL_LENGTH)
	private String profileImageUrl;

	private ContentCast(
		Content content,
		String name,
		int displayOrder,
		String roleName,
		String profileImageUrl
	) {
		this.content = Objects.requireNonNull(content, "content는 필수입니다.");
		this.name = normalizeRequired(name, MAX_NAME_LENGTH, "name");
		this.displayOrder = validateDisplayOrder(displayOrder);
		this.roleName = normalizeOptional(
			roleName,
			MAX_ROLE_NAME_LENGTH,
			"roleName"
		);
		this.profileImageUrl = normalizeOptional(
			profileImageUrl,
			MAX_PROFILE_IMAGE_URL_LENGTH,
			"profileImageUrl"
		);
	}

	public static ContentCast create(
		Content content,
		String name,
		int displayOrder,
		String roleName,
		String profileImageUrl
	) {
		return new ContentCast(
			content,
			name,
			displayOrder,
			roleName,
			profileImageUrl
		);
	}

	private static int validateDisplayOrder(int displayOrder) {
		if (displayOrder < 0) {
			throw new IllegalArgumentException(
				"displayOrder는 0 이상이어야 합니다."
			);
		}
		return displayOrder;
	}

	private static String normalizeRequired(
		String value,
		int maxLength,
		String fieldName
	) {
		if (value == null) {
			throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
		}

		String normalized = value.strip();

		if (normalized.isEmpty()) {
			throw new IllegalArgumentException(
				fieldName + "은(는) 비어 있을 수 없습니다."
			);
		}

		validateLength(normalized, maxLength, fieldName);
		return normalized;
	}

	private static String normalizeOptional(
		String value,
		int maxLength,
		String fieldName
	) {
		if (value == null) {
			return null;
		}

		String normalized = value.strip();

		if (normalized.isEmpty()) {
			return null;
		}

		validateLength(normalized, maxLength, fieldName);
		return normalized;
	}

	private static void validateLength(
		String value,
		int maxLength,
		String fieldName
	) {
		if (value.length() > maxLength) {
			throw new IllegalArgumentException(
				fieldName + "은(는) " + maxLength + "자를 초과할 수 없습니다."
			);
		}
	}
}
