package com.moduplaylist.core.user.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class InvalidUserProfileUpdateException extends BaseException {

  public InvalidUserProfileUpdateException() {
    super(ErrorCode.INVALID_USER_PROFILE_UPDATE);
  }

}
