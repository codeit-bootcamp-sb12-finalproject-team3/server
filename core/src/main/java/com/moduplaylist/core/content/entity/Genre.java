package com.moduplaylist.core.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "genres",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uq_genres_name",
						columnNames = "name"
				),
				@UniqueConstraint(
						name = "uq_genres_external",
						columnNames = {"external_source", "external_id"}
				)
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Genre extends ContentUuidEntity {

	private static final int MAX_NAME_LENGTH = 50;
	private static final int MAX_EXTERNAL_SOURCE_LENGTH = 30;

	@Column(
			name = "name",
			nullable = false,
			length = MAX_NAME_LENGTH
	)
	private String name;

	@Column(
			name = "external_source",
			nullable = false,
			updatable = false,
			length = MAX_EXTERNAL_SOURCE_LENGTH
	)
	private String externalSource;

	@Column(
			name = "external_id",
			nullable = false,
			updatable = false
	)
	private int externalId;

	private Genre(
			String name,
			String externalSource,
			int externalId
	) {
		this.name = normalizeRequired(
				name,
				MAX_NAME_LENGTH,
				"name"
		);
		this.externalSource = normalizeRequired(
				externalSource,
				MAX_EXTERNAL_SOURCE_LENGTH,
				"externalSource"
		);
		this.externalId = externalId;
	}

	public static Genre create(
			String name,
			String externalSource,
			int externalId
	) {
		return new Genre(name, externalSource, externalId);
	}

	public void updateName(String name) {
		this.name = normalizeRequired(
				name,
				MAX_NAME_LENGTH,
				"name"
		);
	}

	private static String normalizeRequired(
			String value,
			int maxLength,
			String fieldName
	) {
		if (value == null) {
			throw new IllegalArgumentException(
					fieldName + "은(는) 필수입니다."
			);
		}

		String normalized = value.strip();

		if (normalized.isEmpty()) {
			throw new IllegalArgumentException(
					fieldName + "은(는) 비어 있을 수 없습니다."
			);
		}

		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException(
					fieldName + "은(는) "
							+ maxLength
							+ "자를 초과할 수 없습니다."
			);
		}

		return normalized;
	}
}