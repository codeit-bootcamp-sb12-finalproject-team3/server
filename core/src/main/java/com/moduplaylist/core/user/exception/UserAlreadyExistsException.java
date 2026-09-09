package com.moduplaylist.core.user.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class UserAlreadyExistsException extends BaseException {

  public UserAlreadyExistsException() {
    super(ErrorCode.USER_ALREADY_EXISTS);
  }
}
