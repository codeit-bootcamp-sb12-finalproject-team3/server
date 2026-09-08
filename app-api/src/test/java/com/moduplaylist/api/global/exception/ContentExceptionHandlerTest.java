package com.moduplaylist.api.global.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ContentExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void missingContentReturns404WithContentId() {
        UUID contentId = UUID.randomUUID();
        BaseException exception = new BaseException(ErrorCode.CONTENT_NOT_FOUND);
        exception.addDetail("contentId", contentId);

        var response = handler.handleBaseException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CONTENT_NOT_FOUND");
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getDetails()).containsEntry("contentId", contentId);
    }

    @Test
    void internalIllegalArgumentDoesNotBecomeClientErrorOrExposeMessage() {
        var response = handler.handleException(new IllegalArgumentException("internal detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().getMessage()).doesNotContain("internal detail");
    }
}
