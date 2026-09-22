package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class ContentUploadLimitExceededException extends BaseException {
	public ContentUploadLimitExceededException() { super(ErrorCode.UPLOAD_LIMIT_EXCEEDED); }
}
