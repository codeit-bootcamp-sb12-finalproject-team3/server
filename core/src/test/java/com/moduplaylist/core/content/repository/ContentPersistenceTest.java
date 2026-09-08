package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:contents;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.test.database.replace=NONE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ContextConfiguration(classes = ContentPersistenceTest.Config.class)
@Import({ContentQueryRepository.class, ContentRelationRepository.class})
class ContentPersistenceTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Content.class)
    @EnableJpaRepositories(basePackageClasses = ContentRepository.class)
    @EnableJpaAuditing
    static class Config {}

    @Autowired ContentRepository contents;
    @Autowired ContentQueryRepository queries;
    @Autowired ContentRelationRepository relations;
    @Autowired EpisodeRepository episodes;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void schema() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS tags(id BINARY(16) PRIMARY KEY,name VARCHAR(100) NOT NULL UNIQUE)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS content_tags(id BINARY(16) PRIMARY KEY,content_id BINARY(16) NOT NULL,tag_id BINARY(16) NOT NULL,source VARCHAR(20) NOT NULL,UNIQUE(content_id,tag_id),FOREIGN KEY(content_id) REFERENCES contents(id) ON DELETE CASCADE,FOREIGN KEY(tag_id) REFERENCES tags(id))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS genres(id BINARY(16) PRIMARY KEY,name VARCHAR(50) NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS content_genres(content_id BINARY(16),genre_id BINARY(16))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS content_casts(content_id BINARY(16),name VARCHAR(100),role_name VARCHAR(255),profile_image_url VARCHAR(500),display_order INT)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS content_likes(content_id BINARY(16),user_id BINARY(16))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS watch_parties(id BINARY(16) PRIMARY KEY,content_id BINARY(16),title VARCHAR(100),scheduled_at TIMESTAMP,status VARCHAR(20),ended_at TIMESTAMP,FOREIGN KEY(content_id) REFERENCES contents(id) ON DELETE RESTRICT)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS watch_party_reminders(watch_party_id BINARY(16),user_id BINARY(16))");
    }

    private Content movie(String title, String rating) {
        return contents.saveAndFlush(Content.builder().title(title).type(ContentType.MOVIE)
                .averageRating(new BigDecimal(rating)).build());
    }

    @Test
    void ratingCursorDoesNotRepeatOrSkipTiesInEitherDirection() {
        movie("A", "4.50");
        movie("B", "4.50");
        movie("C", "4.50");
        for (boolean ascending : List.of(false, true)) {
            var first = queries.search(new ContentSearch(null, null, null, null, null,
                    true, ascending, null, null, null, 1));
            Content last = first.contents().get(0);
            var next = queries.search(new ContentSearch(null, null, null, null, null,
                    true, ascending, null, last.getAverageRating(), last.getId(), 10));
            assertThat(first.totalCount()).isEqualTo(3);
            assertThat(next.contents()).hasSize(2).doesNotContain(last);
            assertThat(next.totalCount()).isEqualTo(3);
        }
    }

    @Test
    void tvSeriesFilterReturnsSeasonsInsteadOfSeries() {
        Content series = contents.saveAndFlush(Content.builder().title("Series").type(ContentType.TV_SERIES).build());
        Content season = contents.saveAndFlush(Content.builder().title("Season").type(ContentType.TV_SEASON)
                .parentContent(series).seasonNumber(0).build());
        movie("Movie", "0.00");
        var result = queries.search(new ContentSearch("tvSeries", null, null, null, null,
                false, false, null, null, null, 20));
        assertThat(result.contents()).extracting(Content::getId).containsExactly(season.getId());
    }

    @Test
    void manualTagReplacementPreservesAiAndAvoidsDuplicates() {
        Content movie = movie("Movie", "0");
        relations.replaceManualTags(movie.getId(), List.of("Growth", "Growth"));
        jdbc.update("UPDATE content_tags SET source='AI' WHERE content_id=?", ContentQueryRepository.bytes(movie.getId()));
        relations.replaceManualTags(movie.getId(), List.of("Growth", "Family"));
        assertThat(relations.tags(movie.getId())).hasSize(2);
        relations.replaceManualTags(movie.getId(), List.of());
        assertThat(relations.tags(movie.getId())).extracting(ContentRelationRepository.Tag::source).containsExactly("AI");
    }

    @Test
    void deletingSeriesCascadesToEpisodes() {
        Content series = contents.saveAndFlush(Content.builder().title("Series").type(ContentType.TV_SERIES).build());
        Content season = contents.saveAndFlush(Content.builder().title("Season").type(ContentType.TV_SEASON)
                .parentContent(series).seasonNumber(1).build());
        episodes.saveAndFlush(Episode.builder().title("Episode").season(season).episodeNumber(1).externalId(1).build());
        contents.deleteByIdInDatabase(series.getId());
        contents.flush();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contents", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM episodes", Long.class)).isZero();
    }

    @Test
    void keywordEscapesWildcardsAndCanFindCastNames() {
        Content literal = movie("100% Fun", "0");
        movie("100 Other", "0");
        var found = queries.search(new ContentSearch(null, null, null, "100%", null,
                false, false, null, null, null, 20));
        assertThat(found.contents()).extracting(Content::getId).containsExactly(literal.getId());
        jdbc.update("INSERT INTO content_casts(content_id,name,display_order) VALUES (?,?,0)",
                ContentQueryRepository.bytes(literal.getId()), "Actor Name");
        var cast = queries.search(new ContentSearch(null, null, null, "Actor", null,
                false, false, null, null, null, 20));
        assertThat(cast.contents()).extracting(Content::getId).containsExactly(literal.getId());
    }

    @Test
    void genreAndLikedUserFiltersAreCombinedWithoutDuplicates() {
        Content match = movie("Match", "0");
        movie("Other", "0");
        UUID genre = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        jdbc.update("INSERT INTO content_genres VALUES (?,?)", ContentQueryRepository.bytes(match.getId()), ContentQueryRepository.bytes(genre));
        jdbc.update("INSERT INTO content_likes VALUES (?,?)", ContentQueryRepository.bytes(match.getId()), ContentQueryRepository.bytes(user));
        var found = queries.search(new ContentSearch(null, genre, null, null, user,
                false, false, null, null, null, 20));
        assertThat(found.contents()).extracting(Content::getId).containsExactly(match.getId());
        assertThat(found.totalCount()).isEqualTo(1);
    }

    @Test
    void watchPartiesHideOldAndEndedAndPrioritizeRecentThenReminders() {
        Content movie = movie("Movie", "0");
        Instant now = Instant.parse("2026-09-08T00:00:00Z");
        UUID recent = insertParty(movie.getId(), now.minusSeconds(1800), "LIVE");
        insertParty(movie.getId(), now.minusSeconds(3600), "LIVE");
        insertParty(movie.getId(), now.plusSeconds(300), "ENDED");
        UUID next = insertParty(movie.getId(), now.plusSeconds(600), "SCHEDULED");
        UUID popular = insertParty(movie.getId(), now.plusSeconds(600), "SCHEDULED");
        jdbc.update("INSERT INTO watch_party_reminders VALUES (?,?)", ContentQueryRepository.bytes(popular), ContentQueryRepository.bytes(UUID.randomUUID()));
        assertThat(relations.watchParties(movie.getId(), now)).extracting(ContentRelationRepository.WatchParty::id)
                .containsExactly(recent, popular, next);
        assertThat(relations.hasWatchParty(movie.getId())).isTrue();
    }

    private UUID insertParty(UUID contentId, Instant scheduledAt, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO watch_parties(id,content_id,title,scheduled_at,status) VALUES (?,?,?,?,?)",
                ContentQueryRepository.bytes(id), ContentQueryRepository.bytes(contentId), "Party", Timestamp.from(scheduledAt), status);
        return id;
    }
}
