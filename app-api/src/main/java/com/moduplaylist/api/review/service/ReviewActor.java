package com.moduplaylist.api.review.service;

import com.moduplaylist.core.common.exception.*;
import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** JWT 연동 시 인증 주체의 사용자 ID 추출을 이 클래스에서 연결한다. */
@Component
public class ReviewActor {
    public UUID userId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new BaseException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        try { return UUID.fromString(auth.getName()); }
        catch (IllegalArgumentException e) { throw new BaseException(ErrorCode.AUTHENTICATION_REQUIRED); }
    }
}
