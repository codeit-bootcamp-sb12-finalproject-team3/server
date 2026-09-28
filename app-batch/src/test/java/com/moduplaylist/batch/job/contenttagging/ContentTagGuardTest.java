package com.moduplaylist.batch.job.contenttagging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.content.ai.ContentTagInput;
import com.moduplaylist.core.content.ai.ContentTaggingException;
import jakarta.validation.Validation;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContentTagGuardTest {

    private final ContentTagGuard guard = new ContentTagGuard(
        new ObjectMapper(),
        Validation.buildDefaultValidatorFactory().getValidator(),
        new ContentTaggingProperties()
    );

    @Test
    void acceptsNormalizedCaseInsensitiveWholeKeywordEvidence() {
        ContentTagInput input = inputWithKeywords("time travel");
        String response = """
            {"tags":[{"name":"시간 여행","evidenceField":"tmdbKeywords",
            "evidenceText":" Time  Travel "}]}
            """;

        assertThat(guard.validate(response, input)).containsExactly("시간 여행");
    }

    @Test
    void rejectsKeywordEvidenceThatOnlyPartiallyMatches() {
        ContentTagInput input = inputWithKeywords("war");
        String response = """
            {"tags":[{"name":"세계 대전","evidenceField":"tmdbKeywords",
            "evidenceText":"world war"}]}
            """;

        assertThatThrownBy(() -> guard.validate(response, input))
            .isInstanceOf(ContentTaggingException.class)
            .hasMessage("VALIDATION_FAILED");
    }

    private static ContentTagInput inputWithKeywords(String keyword) {
        return new ContentTagInput(
            "movie",
            "테스트 작품",
            List.of(),
            "",
            List.of(keyword),
            "MOVIE",
            List.of()
        );
    }
}
