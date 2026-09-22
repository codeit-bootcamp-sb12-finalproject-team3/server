package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class InvalidContentImageException extends BaseException {
	public InvalidContentImageException() { super(ErrorCode.INVALID_IMAGE); }
}
