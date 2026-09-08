package com.moduplaylist.core.review;

import com.moduplaylist.core.content.Content;
import com.moduplaylist.core.content.ContentType;
import com.moduplaylist.core.user.User;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ReviewTest {
    private Review.ReviewBuilder<?, ?> validReview() {
        return Review.builder().user(mock(User.class))
                .content(Content.builder().title("Movie").type(ContentType.MOVIE).build())
                .reviewText("Review").rating(new BigDecimal("4.5"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.5", "1", "1.5", "2", "2.5", "3", "3.5", "4", "4.5", "5.0"})
    void acceptsEveryAllowedRating(String rating) {
        Review review = validReview().rating(new BigDecimal(rating)).build();
        assertThatCode(review::validate).doesNotThrowAnyException();
        assertThat(review.isSpoiler()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.5", "5.5", "4.2", "4.55"})
    void rejectsOutOfRangeAndFractionalStepsBeforeDatabaseRounding(String rating) {
        Review review = validReview().rating(new BigDecimal(rating)).build();
        assertThatIllegalArgumentException().isThrownBy(review::validate);
    }

    @Test
    void ratingAloneIsNotAReview() {
        Review review = validReview().reviewText("  ").build();
        assertThatIllegalArgumentException().isThrownBy(review::validate);
    }

    @Test
    void requiresRatingAndBothAssociations() {
        assertThatIllegalArgumentException().isThrownBy(validReview().rating(null).build()::validate);
        assertThatIllegalArgumentException().isThrownBy(validReview().user(null).build()::validate);
        assertThatIllegalArgumentException().isThrownBy(validReview().content(null).build()::validate);
    }
}
