package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class DuplicateContentConfirmationRequiredException extends BaseException {

	public DuplicateContentConfirmationRequiredException() {
		super(ErrorCode.DUPLICATE_CONFIRMATION_REQUIRED);
	}
}
