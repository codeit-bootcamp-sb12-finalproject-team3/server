package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class PlatformNotFoundException extends BaseException {

	public PlatformNotFoundException(UUID platformId) {
		super(ErrorCode.PLATFORM_NOT_FOUND);
		addDetail("platformId", platformId);
	}
}
