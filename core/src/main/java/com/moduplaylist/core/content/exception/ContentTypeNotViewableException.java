package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.content.entity.ContentType;
import java.util.UUID;

public class ContentTypeNotViewableException extends BaseException {

	public ContentTypeNotViewableException(UUID contentId, ContentType contentType) {
		super(ErrorCode.CONTENT_TYPE_NOT_VIEWABLE);
		addDetail("contentId", contentId);
		addDetail("contentType", contentType.getValue());
	}
}
