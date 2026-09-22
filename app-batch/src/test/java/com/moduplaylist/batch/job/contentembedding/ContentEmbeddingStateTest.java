package com.moduplaylist.batch.job.contentembedding;

import static org.assertj.core.api.Assertions.assertThat;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ContentEmbeddingStateTest {

    @Test
    void newMovieAndSeasonArePendingButNonEmbeddableTypesAreNot() {
        Content series = content(ContentType.TV_SERIES);
        Content movie = content(ContentType.MOVIE);
        Content season = Content.builder()
                .parentContent(series)
                .title("season")
                .type(ContentType.TV_SEASON)
                .seasonNumber(1)
                .build();
        Content sport = content(ContentType.SPORT);

        assertThat(movie.isEmbeddingPending()).isTrue();
        assertThat(season.isEmbeddingPending()).isTrue();
        assertThat(series.isEmbeddingPending()).isFalse();
        assertThat(sport.isEmbeddingPending()).isFalse();
    }

    @Test
    void sourceChangeAdvancesTimestampAndSetsPending() throws InterruptedException {
        Content movie = content(ContentType.MOVIE);
        Instant before = movie.getEmbeddingSourceUpdatedAt();
        Thread.sleep(1L);

        movie.markEmbeddingSourceUpdated();

        assertThat(movie.getEmbeddingSourceUpdatedAt()).isAfter(before);
        assertThat(movie.isEmbeddingPending()).isTrue();
    }

    private Content content(ContentType type) {
        return Content.builder()
                .title(type.getValue())
                .type(type)
                .build();
    }
}
