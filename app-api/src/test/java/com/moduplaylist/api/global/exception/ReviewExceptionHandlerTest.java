package com.moduplaylist.api.global.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @ParameterizedTest
    @CsvSource({
            "REVIEW_NOT_FOUND,404",
            "REVIEW_ALREADY_EXISTS,409",
            "REVIEW_ACCESS_DENIED,403"
    })
    void returnsReviewErrorWithStatusAndDetails(ErrorCode code, int status) {
        UUID contentId = UUID.randomUUID();
        BaseException exception = new BaseException(code);
        exception.addDetail("contentId", contentId);

        var response = handler.handleBaseException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(status);
        assertThat(response.getBody().getCode()).isEqualTo(code.name());
        assertThat(response.getBody().getMessage()).isEqualTo(code.getMessage());
        assertThat(response.getBody().getDetails()).containsEntry("contentId", contentId);
    }
}
