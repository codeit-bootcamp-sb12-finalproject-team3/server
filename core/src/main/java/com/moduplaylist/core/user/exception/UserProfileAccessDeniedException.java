package com.moduplaylist.core.user.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class UserProfileAccessDeniedException extends BaseException {

  public UserProfileAccessDeniedException(UUID userId) {
    super(ErrorCode.USER_PROFILE_ACCESS_DENIED);
    addDetail("userId", userId);
  }

}
