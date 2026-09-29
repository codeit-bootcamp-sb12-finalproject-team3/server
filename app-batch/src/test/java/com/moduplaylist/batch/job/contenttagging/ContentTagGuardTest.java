package com.moduplaylist.batch.job.contenttagging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.content.ai.ContentTagInput;
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
    void acceptsSummarizedKeywordEvidence() {
        ContentTagInput input = inputWithKeywords("time travel");
        String response = """
            {"tags":[{"name":"시간 여행","evidenceField":"tmdbKeywords",
            "evidenceText":"시간을 오가는 소재"}]}
            """;

        assertThat(guard.validate(response, input)).containsExactly("시간 여행");
    }

    @Test
    void acceptsKeywordEvidenceThatDoesNotExactlyMatch() {
        ContentTagInput input = inputWithKeywords("war");
        String response = """
            {"tags":[{"name":"세계 대전","evidenceField":"tmdbKeywords",
            "evidenceText":"world war"}]}
            """;

        assertThat(guard.validate(response, input)).containsExactly("세계 대전");
    }

    @Test
    void acceptsDescriptionEvidenceThatParaphrasesTheInput() {
        ContentTagInput input = new ContentTagInput(
            "movie",
            "테스트 작품",
            List.of(),
            "가족을 지키기 위해 낯선 도시로 떠나는 주인공의 이야기",
            List.of(),
            "MOVIE",
            List.of()
        );
        String response = """
            {"tags":[{"name":"가족애","evidenceField":"description",
            "evidenceText":"가족을 위한 주인공의 여정"}]}
            """;

        assertThat(guard.validate(response, input)).containsExactly("가족애");
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
