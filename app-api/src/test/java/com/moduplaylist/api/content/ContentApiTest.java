package com.moduplaylist.api.content;

import com.moduplaylist.api.content.controller.ContentController;
import com.moduplaylist.api.content.dto.*;
import com.moduplaylist.api.content.service.ContentService;
import com.moduplaylist.api.content.service.impl.ContentServiceImpl;
import com.moduplaylist.api.global.exception.GlobalExceptionHandler;
import com.moduplaylist.core.content.*;
import com.moduplaylist.core.content.repository.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(ContentApiTest.Config.class)
@WithMockUser(roles = "ADMIN")
class ContentApiTest {
    @org.springframework.boot.test.context.TestConfiguration
    @EnableMethodSecurity
    static class Config {
        @Bean static MethodValidationPostProcessor validation() { return new MethodValidationPostProcessor(); }
        @Bean ContentRepository contents() { return mock(ContentRepository.class); }
        @Bean EpisodeRepository episodes() { return mock(EpisodeRepository.class); }
        @Bean ContentQueryRepository queries() { return mock(ContentQueryRepository.class); }
        @Bean ContentRelationRepository relations() { return mock(ContentRelationRepository.class); }
        @Bean ContentService service(ContentRepository c, EpisodeRepository e, ContentQueryRepository q,
                ContentRelationRepository r, ApplicationEventPublisher p) {
            return new ContentServiceImpl(c, e, q, r, p);
        }
        @Bean ContentController controller(ContentService service) { return new ContentController(service); }
    }

    @Autowired ContentController controller;
    @Autowired ContentService service;
    @Autowired ContentRepository contents;
    @Autowired EpisodeRepository episodes;
    @Autowired ContentQueryRepository queries;
    @Autowired ContentRelationRepository relations;
    MockMvc mvc;
    UUID id;
    Content movie;

    @BeforeEach
    void setUp() {
        reset(contents, episodes, queries, relations);
        id = UUID.randomUUID();
        movie = Content.builder().id(id).type(ContentType.MOVIE).title("Movie")
                .createdAt(Instant.parse("2026-09-08T00:00:00Z")).build();
        when(contents.findById(id)).thenReturn(Optional.of(movie));
        when(contents.findByIdForUpdate(id)).thenReturn(Optional.of(movie));
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void readsContentWithoutExposingExternalIdentifiers() throws Exception {
        mvc.perform(get("/api/contents/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("movie"))
                .andExpect(jsonPath("$.externalId").doesNotExist());
    }

    @Test
    void missingContentIs404() throws Exception {
        mvc.perform(get("/api/contents/" + UUID.randomUUID()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CONTENT_NOT_FOUND"));
    }

    @Test
    void invalidIdAndMalformedJsonAre400() throws Exception {
        mvc.perform(get("/api/contents/bad-id")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/contents").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidSeasonRequestIs400BeforeSave() throws Exception {
        mvc.perform(post("/api/contents").contentType("application/json")
                .content("{\"title\":\"Season\",\"type\":\"tvSeason\",\"seasonNumber\":1}"))
                .andExpect(status().isBadRequest());
        verify(contents, never()).saveAndFlush(any());
    }

    @Test
    void adminCreateReturns201AndLocation() throws Exception {
        when(contents.saveAndFlush(any())).thenAnswer(call -> {
            Content created = call.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(created, "id", id);
            return created;
        });
        mvc.perform(post("/api/contents").contentType("application/json")
                .content("{\"title\":\"New\",\"type\":\"movie\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/contents/" + id))
                .andExpect(jsonPath("$.title").value("New"));
    }

    @Test
    void duplicateSeasonReturns409() throws Exception {
        Content series = Content.builder().id(id).type(ContentType.TV_SERIES).title("Series").build();
        when(contents.findByIdForUpdate(id)).thenReturn(Optional.of(series));
        when(contents.existsByParentContent_IdAndSeasonNumber(id, 1)).thenReturn(true);
        mvc.perform(post("/api/contents").contentType("application/json")
                .content("{\"title\":\"Season\",\"type\":\"tvSeason\",\"parentContentId\":\"" + id + "\",\"seasonNumber\":1}"))
                .andExpect(status().isConflict());
        verify(contents, never()).saveAndFlush(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void nonAdminCannotCreateUpdateOrDelete() throws Exception {
        mvc.perform(post("/api/contents").contentType("application/json")
                .content("{\"title\":\"Movie\",\"type\":\"movie\"}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/contents/" + id).contentType("application/json")
                .content("{\"title\":\"Updated\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/contents/" + id)).andExpect(status().isForbidden());
        verifyNoInteractions(contents);
    }

    @Test
    @WithMockUser(roles = "USER")
    void serviceAlsoEnforcesAdminPermission() {
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void adminUpdatePreservesStatsAndReplacesOnlyManualTags() throws Exception {
        mvc.perform(patch("/api/contents/" + id).contentType("application/json")
                .content("{\"title\":\"Updated\",\"tags\":[\" Growth \",\"Growth\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.likeCount").value(0));
        verify(relations).replaceManualTags(id, List.of("Growth"));
    }

    @Test
    void incompatibleTypeChangeIs400() throws Exception {
        mvc.perform(patch("/api/contents/" + id).contentType("application/json")
                .content("{\"type\":\"sport\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void blocksDeletionWhenChildSeasonHasWatchParty() throws Exception {
        UUID seasonId = UUID.randomUUID();
        Content season = Content.builder().id(seasonId).type(ContentType.TV_SEASON)
                .title("Season").parentContent(movie).seasonNumber(1).build();
        when(contents.findAllByParentContent_IdOrderBySeasonNumberAsc(id)).thenReturn(List.of(season));
        when(contents.findByIdForUpdate(seasonId)).thenReturn(Optional.of(season));
        when(relations.hasWatchParty(seasonId)).thenReturn(true);
        mvc.perform(delete("/api/contents/" + id)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONTENT_DELETE_RESTRICTED"));
        verify(contents, never()).deleteByIdInDatabase(any());
    }

    @Test
    void adminDeleteReturns204() throws Exception {
        mvc.perform(delete("/api/contents/" + id)).andExpect(status().isNoContent());
        verify(contents).deleteByIdInDatabase(id);
        verify(contents).flush();
    }

    @Test
    void invalidCursorIs400AndNeverQueriesDatabase() throws Exception {
        mvc.perform(get("/api/contents").param("cursor", "garbage").param("idAfter", id.toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/contents").param("cursor", "2026-09-08T00:00:00Z"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(queries);
    }

    @Test
    void cursorPageUsesLimitPlusOneAndStableId() throws Exception {
        when(queries.search(any())).thenReturn(new ContentQueryRepository.SearchResult(List.of(movie, movie), 2));
        mvc.perform(get("/api/contents").param("limit", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextIdAfter").value(id.toString()))
                .andExpect(jsonPath("$.nextCursor").value("2026-09-08T00:00:00Z"));
    }
}
