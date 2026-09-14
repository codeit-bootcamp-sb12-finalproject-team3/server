package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class GenreNotFoundException extends BaseException {

    public GenreNotFoundException(UUID genreId) {
        super(ErrorCode.GENRE_NOT_FOUND);
        addDetail("genreId", genreId);
    }
}
