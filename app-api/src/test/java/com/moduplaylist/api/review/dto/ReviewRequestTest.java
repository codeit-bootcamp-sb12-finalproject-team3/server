package com.moduplaylist.api.review.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewRequestTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.5", "4.5", "5"})
    void acceptsValidCreate(String rating) {
        var request = ReviewCreateRequest.builder().contentId(UUID.randomUUID())
                .reviewText("Review").rating(new BigDecimal(rating)).build();
        assertThat(VALIDATOR.validate(request)).isEmpty();
        assertThat(request.isSpoiler()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.5", "5.5", "4.2", "4.55"})
    void rejectsInvalidRatingsInBothRequests(String rating) {
        var create = ReviewCreateRequest.builder().contentId(UUID.randomUUID())
                .reviewText("Review").rating(new BigDecimal(rating)).build();
        var update = ReviewUpdateRequest.builder().rating(new BigDecimal(rating)).build();
        assertThat(VALIDATOR.validate(create)).isNotEmpty();
        assertThat(VALIDATOR.validate(update)).isNotEmpty();
    }

    @Test
    void requiresCreateFieldsAndRejectsBlankUpdateText() {
        assertThat(VALIDATOR.validate(new ReviewCreateRequest())).hasSize(3);
        var request = ReviewUpdateRequest.builder().reviewText(" \n ").build();
        assertThat(VALIDATOR.validate(request)).isNotEmpty();
    }

    @Test
    void jsonDistinguishesFalseFromOmittedSpoiler() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var omitted = mapper.readValue("{}", ReviewUpdateRequest.class);
        var cleared = mapper.readValue("{\"spoiler\":false}", ReviewUpdateRequest.class);
        assertThat(omitted.getSpoiler()).isNull();
        assertThat(cleared.getSpoiler()).isFalse();
        assertThat(VALIDATOR.validate(cleared)).isEmpty();
        assertThat(mapper.writeValueAsString(cleared)).doesNotContain("ratingStepValid");
    }

    @Test
    void validatesPaginationDefaultsAndBounds() {
        var request = ReviewListRequest.builder().build();
        assertThat(VALIDATOR.validate(request)).isEmpty();
        assertThat(request.getLimit()).isEqualTo(20);
        request.setLimit(0);
        assertThat(VALIDATOR.validate(request)).isNotEmpty();
        request.setLimit(101);
        assertThat(VALIDATOR.validate(request)).isNotEmpty();
    }
}
