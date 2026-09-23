package com.moduplaylist.core.common.exception;

public class ImageUploadLimitExceededException extends BaseException {

	public ImageUploadLimitExceededException() {
		super(ErrorCode.UPLOAD_LIMIT_EXCEEDED);
	}
}
