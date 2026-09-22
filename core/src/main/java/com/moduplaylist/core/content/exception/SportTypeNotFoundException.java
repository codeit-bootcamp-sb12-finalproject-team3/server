package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class SportTypeNotFoundException extends BaseException {

	public SportTypeNotFoundException(UUID sportTypeId) {
		super(ErrorCode.SPORT_TYPE_NOT_FOUND);
		addDetail("sportTypeId", sportTypeId);
	}
}
