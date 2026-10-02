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
            List.of(),
            List.of()
        );
        String response = """
            {"tags":[{"name":"가족애","evidenceField":"description",
            "evidenceText":"가족을 위한 주인공의 여정"}]}
            """;

        assertThat(guard.validate(response, input)).containsExactly("가족애");
    }

    @Test
    void rejectsDisallowedEnglishTokensMixedWithKorean() {
        ContentTagInput input = inputWithDescription("연예인의 일상과 경쟁, 신분 변화를 다룬 이야기");
        String response = """
            {"tags":[
              {"name":"세Celebrity 밀착 서비스","evidenceField":"description","evidenceText":"연예인의 일상"},
              {"name":"경쟁과 rivalry","evidenceField":"description","evidenceText":"인물들의 경쟁"},
              {"name":"신분 변 disguise","evidenceField":"description","evidenceText":"신분을 바꾸는 설정"}
            ]}
            """;

        assertThatThrownBy(() -> guard.validate(response, input))
            .isInstanceOf(ContentTaggingException.class)
            .hasMessage("VALIDATION_FAILED");
    }

    @Test
    void acceptsAllowedEnglishAbbreviations() {
        ContentTagInput input = inputWithDescription("VR 공연과 SNS 인플루언서를 다룬 이야기");
        String response = """
            {"tags":[
              {"name":"VR 콘서트","evidenceField":"description","evidenceText":"VR 공연"},
              {"name":"SNS 인플루언서","evidenceField":"description","evidenceText":"SNS 인플루언서"}
            ]}
            """;

        assertThat(guard.validate(response, input)).containsExactly("VR 콘서트", "SNS 인플루언서");
    }

    @Test
    void acceptsKPopAsAllowedHyphenatedAbbreviation() {
        ContentTagInput input = inputWithDescription("K-pop 아이돌의 성장을 다룬 이야기");
        String response = """
            {"tags":[{"name":"K-pop 아이돌","evidenceField":"description",
            "evidenceText":"K-pop 아이돌의 성장"}]}
            """;

        assertThat(guard.validate(response, input)).containsExactly("K-pop 아이돌");
    }

    @Test
    void excludesCurrentTagsButAllowsSeriesCandidates() {
        ContentTagInput input = new ContentTagInput(
            "tvSeason",
            "테스트 작품",
            List.of(),
            "가족을 통해 사회를 풍자하는 이야기",
            List.of(),
            "SERIES",
            List.of("가족애"),
            List.of("사회 풍자")
        );
        String response = """
            {"tags":[
              {"name":"가족애","evidenceField":"description","evidenceText":"가족 이야기"},
              {"name":"사회 풍자","evidenceField":"description","evidenceText":"사회를 풍자하는 이야기"}
            ]}
            """;

        assertThat(guard.validate(response, input)).containsExactly("사회 풍자");
    }

    private static ContentTagInput inputWithKeywords(String keyword) {
        return new ContentTagInput(
            "movie",
            "테스트 작품",
            List.of(),
            "",
            List.of(keyword),
            "MOVIE",
            List.of(),
            List.of()
        );
    }

    private static ContentTagInput inputWithDescription(String description) {
        return new ContentTagInput(
            "movie",
            "테스트 작품",
            List.of(),
            description,
            List.of(),
            "MOVIE",
            List.of(),
            List.of()
        );
    }
}
