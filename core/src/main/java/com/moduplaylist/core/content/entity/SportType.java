package com.moduplaylist.core.content.entity;

import com.moduplaylist.core.common.BaseEntity;
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
	name = "sport_types",
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_sport_types_code", columnNames = "code"),
		@UniqueConstraint(name = "uq_sport_types_name", columnNames = "name")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SportType extends BaseEntity {

	private static final int MAX_CODE_LENGTH = 50;
	private static final int MAX_NAME_LENGTH = 100;

	@Column(nullable = false, length = MAX_CODE_LENGTH)
	private String code;

	@Column(nullable = false, length = MAX_NAME_LENGTH)
	private String name;

	private SportType(String code, String name) {
		this.code = normalizeRequired(code, MAX_CODE_LENGTH, "code");
		this.name = normalizeRequired(name, MAX_NAME_LENGTH, "name");
	}

	public static SportType create(String code, String name) {
		return new SportType(code, name);
	}

	public void updateName(String name) {
		this.name = normalizeRequired(name, MAX_NAME_LENGTH, "name");
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
		if (normalized.isEmpty() || normalized.length() > maxLength) {
			throw new IllegalArgumentException(
				fieldName + "은(는) " + maxLength + "자 이하여야 합니다."
			);
		}
		return normalized;
	}
}
