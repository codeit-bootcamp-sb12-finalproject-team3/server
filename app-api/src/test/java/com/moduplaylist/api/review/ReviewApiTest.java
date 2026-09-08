package com.moduplaylist.api.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.api.global.config.SecurityConfig;
import com.moduplaylist.api.global.exception.GlobalExceptionHandler;
import com.moduplaylist.api.review.controller.ReviewController;
import com.moduplaylist.api.review.dto.ReviewCreateRequest;
import com.moduplaylist.api.review.service.*;
import com.moduplaylist.api.review.service.impl.ReviewServiceImpl;
import com.moduplaylist.core.content.*;
import com.moduplaylist.core.content.repository.*;
import com.moduplaylist.core.review.Review;
import com.moduplaylist.core.review.repository.*;
import com.moduplaylist.core.user.User;
import com.moduplaylist.core.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ReviewApiTest.Config.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:reviews;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@WithMockUser(username = "00000000-0000-0000-0000-000000000001")
class ReviewApiTest {
    @org.springframework.boot.test.context.TestConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {Content.class, Review.class, User.class})
    @EnableJpaRepositories(basePackageClasses = {ContentRepository.class, ReviewRepository.class, UserRepository.class})
    @EnableJpaAuditing
    @Import({ReviewController.class, ReviewServiceImpl.class, ReviewActor.class,
            ReviewQueryRepository.class, GlobalExceptionHandler.class, SecurityConfig.class})
    static class Config {
        @Bean static MethodValidationPostProcessor validation() { return new MethodValidationPostProcessor(); }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ContentRepository contents;
    @Autowired ReviewRepository reviews;
    @Autowired ReviewService service;
    @Autowired JdbcTemplate jdbc;
    UUID contentId;
    static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM reviews");
        jdbc.update("DELETE FROM contents");
        jdbc.update("DELETE FROM users");
        jdbc.update("INSERT INTO users(id) VALUES (?)", ContentQueryRepository.bytes(USER));
        contentId = contents.saveAndFlush(Content.builder().title("Movie").type(ContentType.MOVIE).build()).getId();
    }

    String create(boolean spoiler) throws Exception {
        String result = mvc.perform(post("/api/reviews").contentType("application/json")
                .content("{\"contentId\":\"" + contentId + "\",\"reviewText\":\"Text\",\"rating\":4.5,\"spoiler\":" + spoiler + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(result).get("id").asText();
    }

    @Test
    void createUpdateDeleteRefreshStatisticsAndPreserveLikes() throws Exception {
        jdbc.update("UPDATE contents SET like_count=7 WHERE id=?", ContentQueryRepository.bytes(contentId));
        String id = create(false);
        assertThat(contents.findById(contentId).orElseThrow().getAverageRating()).isEqualByComparingTo("4.50");
        assertThat(contents.findById(contentId).orElseThrow().getReviewCount()).isEqualTo(1);
        mvc.perform(patch("/api/reviews/" + id).contentType("application/json").content("{\"rating\":2.5}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reviewText").value("Text"));
        assertThat(contents.findById(contentId).orElseThrow().getAverageRating()).isEqualByComparingTo("2.50");
        mvc.perform(delete("/api/reviews/" + id)).andExpect(status().isNoContent());
        Content saved = contents.findById(contentId).orElseThrow();
        assertThat(saved.getAverageRating()).isEqualByComparingTo("0.00");
        assertThat(saved.getReviewCount()).isZero();
        assertThat(saved.getLikeCount()).isEqualTo(7);
    }

    @Test
    void duplicateReturns409WithoutChangingStatistics() throws Exception {
        create(false);
        mvc.perform(post("/api/reviews").contentType("application/json")
                .content("{\"contentId\":\"" + contentId + "\",\"reviewText\":\"Again\",\"rating\":1}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REVIEW_ALREADY_EXISTS"));
        assertThat(reviews.count()).isEqualTo(1);
        assertThat(contents.findById(contentId).orElseThrow().getAverageRating()).isEqualByComparingTo("4.50");
    }

    @Test
    void spoilerHiddenByDefaultAndRevealedExplicitly() throws Exception {
        String id = create(true);
        mvc.perform(get("/api/reviews/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.reviewText").isEmpty());
        mvc.perform(get("/api/reviews/" + id).param("includeSpoilers", "true"))
                .andExpect(jsonPath("$.reviewText").value("Text"));
        mvc.perform(get("/api/reviews").param("contentIdEqual", contentId.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].reviewText").isEmpty());
    }

    @Test
    void otherUserCannotUpdateOrDelete() throws Exception {
        String id = create(false);
        String otherUser = UUID.randomUUID().toString();
        mvc.perform(patch("/api/reviews/" + id)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(otherUser))
                .contentType("application/json").content("{\"rating\":0}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/reviews/" + id)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(otherUser)))
                .andExpect(status().isForbidden());
        assertThat(reviews.count()).isEqualTo(1);
    }

    @Test
    @WithAnonymousUser
    void anonymousCannotWrite() throws Exception {
        mvc.perform(post("/api/reviews").contentType("application/json")
                .content("{\"contentId\":\"" + contentId + "\",\"reviewText\":\"Text\",\"rating\":3}"))
                .andExpect(status().isUnauthorized());
        assertThat(reviews.count()).isZero();
    }

    @Test
    void invalidInputAndMissingReview() throws Exception {
        mvc.perform(post("/api/reviews").contentType("application/json")
                .content("{\"contentId\":\"" + contentId + "\",\"reviewText\":\"Text\",\"rating\":4.2}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/reviews/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/reviews").param("cursor", "bad").param("idAfter", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cursorFiltersAndTieBreakAreStable() throws Exception {
        String firstId = create(false);
        UUID secondUser = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id) VALUES (?)", ContentQueryRepository.bytes(secondUser));
        UUID secondId = UUID.randomUUID();
        jdbc.update("INSERT INTO reviews(id,user_id,content_id,content,rating,is_spoiler,created_at,updated_at) VALUES (?,?,?,?,?,false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                ContentQueryRepository.bytes(secondId), ContentQueryRepository.bytes(secondUser),
                ContentQueryRepository.bytes(contentId), "Second", new BigDecimal("3.0"));
        jdbc.update("UPDATE reviews SET created_at=TIMESTAMP '2026-09-08 00:00:00'");
        var first = mapper.readTree(mvc.perform(get("/api/reviews").param("limit", "1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var next = mapper.readTree(mvc.perform(get("/api/reviews").param("limit", "1")
                .param("cursor", first.get("nextCursor").asText()).param("idAfter", first.get("nextIdAfter").asText()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(next.get("data").get(0).get("id").asText()).isNotEqualTo(first.get("data").get(0).get("id").asText());
        mvc.perform(get("/api/reviews").param("userIdEqual", USER.toString()).param("contentIdEqual", contentId.toString()))
                .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].id").value(firstId));
    }

    @Test
    void concurrentCreatesKeepAccurateAverageAndCount() throws Exception {
        UUID other = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id) VALUES (?)", ContentQueryRepository.bytes(other));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (UUID user : List.of(USER, other)) {
                futures.add(pool.submit(() -> {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(user.toString(), "n/a", List.of()));
                    ready.countDown();
                    try {
                        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                        service.create(ReviewCreateRequest.builder().contentId(contentId).reviewText("Concurrent")
                                .rating(user.equals(USER) ? new BigDecimal("4.5") : new BigDecimal("2.0")).build());
                    } catch (InterruptedException e) { throw new RuntimeException(e); }
                    finally { SecurityContextHolder.clearContext(); }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) future.get(20, TimeUnit.SECONDS);
            Content saved = contents.findById(contentId).orElseThrow();
            assertThat(saved.getReviewCount()).isEqualTo(2);
            assertThat(saved.getAverageRating()).isEqualByComparingTo("3.25");
        } finally { start.countDown(); pool.shutdownNow(); }
    }
}
