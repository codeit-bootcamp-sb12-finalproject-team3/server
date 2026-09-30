package com.moduplaylist.api.trending.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moduplaylist.core.activity.enums.ContentActivityType;
import com.moduplaylist.infrastructure.kafka.event.ContentActivityKafkaEvent;
import com.moduplaylist.infrastructure.trending.TrendingProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TrendingScorePolicyTest {

    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CONTENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final Instant OCCURRED_AT = Instant.parse("2026-09-23T00:00:00Z");

    private TrendingScorePolicy scorePolicy;

    @BeforeEach
    void setUp() {
        scorePolicy = new TrendingScorePolicy(new TrendingProperties());
    }

    @ParameterizedTest
    @CsvSource({
            "CONTENT_VIEW, 0.1",
            "CONTENT_LIKE, 1.5",
            "CONTENT_UNLIKE, -1.5",
            "PLAYLIST_CONTENT_ADDED, 0.7",
            "PLAYLIST_CONTENT_REMOVED, -0.7"
    })
    void calculateReturnsConfiguredActivityWeight(
            ContentActivityType eventType,
            double expected
    ) {
        assertThat(scorePolicy.calculate(event(eventType))).isEqualTo(expected);
    }

    @Test
    void watchPartyParticipationReturnsConfiguredWeight() {
        assertThat(scorePolicy.watchPartyParticipation()).isEqualTo(1.0);
    }

    @ParameterizedTest
    @CsvSource({"0.5", "1.0", "2.5", "3.5", "4.5", "5.0"})
    void ratingCreationUsesFixedWeightRegardlessOfRating(String newRating) {
        assertThat(scorePolicy.calculate(ratingEvent(null, newRating))).isEqualTo(1.0);
    }

    @Test
    void ratingIncreaseKeepsExistingContribution() {
        assertThat(scorePolicy.calculate(ratingEvent("2.5", "5.0"))).isZero();
    }

    @Test
    void ratingDecreaseKeepsExistingContribution() {
        assertThat(scorePolicy.calculate(ratingEvent("5.0", "1.0"))).isZero();
    }

    @Test
    void ratingDeletionRemovesOldContribution() {
        assertThat(scorePolicy.calculate(ratingEvent("4.0", null))).isEqualTo(-1.0);
    }

    @Test
    void ratingEventWithoutOldAndNewRatingIsRejected() {
        assertThatThrownBy(() -> scorePolicy.calculate(ratingEvent(null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("평점 활동에는 이전 평점 또는 신규 평점이 필요합니다.");
    }

    private ContentActivityKafkaEvent event(ContentActivityType eventType) {
        return new ContentActivityKafkaEvent(
                EVENT_ID,
                eventType,
                USER_ID,
                CONTENT_ID,
                OCCURRED_AT
        );
    }

    private ContentActivityKafkaEvent ratingEvent(String oldRating, String newRating) {
        return new ContentActivityKafkaEvent(
                EVENT_ID,
                ContentActivityType.CONTENT_RATING,
                USER_ID,
                CONTENT_ID,
                decimal(oldRating),
                decimal(newRating),
                OCCURRED_AT
        );
    }

    private BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
