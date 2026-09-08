package com.moduplaylist.api.content.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentRequestTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Test
    void acceptsSeasonZeroButRequiresParent() {
        var request = ContentCreateRequest.builder().title("Special").type("tvSeason")
                .seasonNumber(0).parentContentId(UUID.randomUUID()).build();
        assertThat(VALIDATOR.validate(request)).isEmpty();
        request.setParentContentId(null);
        assertThat(VALIDATOR.validate(request)).isNotEmpty();
    }

    @Test
    void sportRequiresSportTypeAndMovieRejectsIt() {
        var request = ContentCreateRequest.builder().title("Match").type("sport").build();
        assertThat(VALIDATOR.validate(request)).isNotEmpty();
        request.setSportType("Football");
        assertThat(VALIDATOR.validate(request)).isEmpty();
        request.setType("movie");
        assertThat(VALIDATOR.validate(request)).isNotEmpty();
    }

    @Test
    void partialUpdateAllowsOmittedTitleButRejectsBlankTitleAndTags() {
        var request = ContentUpdateRequest.builder().description("Updated").build();
        assertThat(VALIDATOR.validate(request)).isEmpty();
        request.setTitle("  ");
        assertThat(VALIDATOR.validate(request)).isNotEmpty();
        request.setTitle(null);
        request.setTags(List.of(" "));
        assertThat(VALIDATOR.validate(request)).isNotEmpty();
        request.setTags(List.of());
        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void jsonUsesDocumentedTypeAndDoesNotExposeValidationProperty() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var request = mapper.readValue("{\"title\":\"Movie\",\"type\":\"movie\"}",
                ContentCreateRequest.class);
        assertThat(VALIDATOR.validate(request)).isEmpty();
        assertThat(mapper.writeValueAsString(request)).doesNotContain("typeFieldsValid");
    }

    @Test
    void paginationDefaultsAreValidAndLimitIsBounded() {
        var request = new ContentListRequest();
        assertThat(request.getLimit()).isEqualTo(20);
        assertThat(VALIDATOR.validate(request)).isEmpty();
        request.setLimit(101);
        assertThat(VALIDATOR.validate(request)).isNotEmpty();
    }
}
