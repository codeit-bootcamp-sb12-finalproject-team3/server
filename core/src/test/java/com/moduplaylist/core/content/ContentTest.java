package com.moduplaylist.core.content;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ContentTest {
    @Test
    void newMovieHasZeroStatisticsAndAllowsManualSource() {
        Content movie = Content.builder().title("영화").type(ContentType.MOVIE).build();

        assertThatCode(movie::validate).doesNotThrowAnyException();
        assertThat(movie.getAverageRating()).isEqualByComparingTo("0.00");
        assertThat(movie.getLikeCount()).isZero();
        assertThat(movie.getReviewCount()).isZero();
    }

    @Test
    void specialSeasonZeroIsAllowedUnderSeries() {
        Content series = Content.builder().title("시리즈").type(ContentType.TV_SERIES).build();
        Content season = Content.builder().title("스페셜").type(ContentType.TV_SEASON)
                .parentContent(series).seasonNumber(0).episodeCount(0).build();

        assertThatCode(season::validate).doesNotThrowAnyException();
    }

    @Test
    void seasonCannotBelongToMovie() {
        Content movie = Content.builder().title("영화").type(ContentType.MOVIE).build();
        Content season = Content.builder().title("시즌").type(ContentType.TV_SEASON)
                .parentContent(movie).seasonNumber(1).build();

        assertThatIllegalArgumentException().isThrownBy(season::validate);
    }

    @Test
    void externalSourceAndIdMustBeProvidedTogether() {
        Content movie = Content.builder().title("영화").type(ContentType.MOVIE)
                .externalSource("TMDB").build();

        assertThatIllegalArgumentException().isThrownBy(movie::validate);
    }

    @Test
    void sportRequiresSportType() {
        Content sport = Content.builder().title("경기").type(ContentType.SPORT).build();

        assertThatIllegalArgumentException().isThrownBy(sport::validate);
    }

    @Test
    void episodeMustBelongToSeasonRatherThanSeries() {
        Content series = Content.builder().title("시리즈").type(ContentType.TV_SERIES).build();
        Episode episode = Episode.builder().title("1화").episodeNumber(1)
                .externalId(100).season(series).build();

        assertThatIllegalArgumentException().isThrownBy(episode::validate);
    }

    @Test
    void specialEpisodeZeroAndUnknownRuntimeAreAllowed() {
        Content series = Content.builder().title("시리즈").type(ContentType.TV_SERIES).build();
        Content season = Content.builder().title("시즌").type(ContentType.TV_SEASON)
                .parentContent(series).seasonNumber(1).build();
        Episode episode = Episode.builder().title("스페셜").episodeNumber(0)
                .externalId(100).season(season).build();

        assertThatCode(episode::validate).doesNotThrowAnyException();
    }
}
