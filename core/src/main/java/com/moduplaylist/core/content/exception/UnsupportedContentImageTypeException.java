package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class UnsupportedContentImageTypeException extends BaseException {
	public UnsupportedContentImageTypeException() { super(ErrorCode.UNSUPPORTED_IMAGE_TYPE); }
}
