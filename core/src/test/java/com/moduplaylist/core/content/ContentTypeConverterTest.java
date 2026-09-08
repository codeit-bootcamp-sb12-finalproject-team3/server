package com.moduplaylist.core.content;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ContentTypeConverterTest {
    private final ContentTypeConverter converter = new ContentTypeConverter();

    @ParameterizedTest
    @CsvSource({"MOVIE,movie", "TV_SERIES,tvSeries", "TV_SEASON,tvSeason", "SPORT,sport"})
    void matchesDatabaseEnumValues(ContentType type, String databaseValue) {
        assertThat(converter.convertToDatabaseColumn(type)).isEqualTo(databaseValue);
        assertThat(converter.convertToEntityAttribute(databaseValue)).isEqualTo(type);
    }

    @Test
    void rejectsUnknownDatabaseValue() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> converter.convertToEntityAttribute("unknown"));
    }

    @Test
    void preservesNullForPersistenceLayer() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
