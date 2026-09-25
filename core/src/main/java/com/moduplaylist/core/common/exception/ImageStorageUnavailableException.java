package com.moduplaylist.core.common.exception;

public class ImageStorageUnavailableException extends BaseException {

	public ImageStorageUnavailableException(Throwable cause) {
		super(ErrorCode.IMAGE_STORAGE_UNAVAILABLE, cause);
	}
}
