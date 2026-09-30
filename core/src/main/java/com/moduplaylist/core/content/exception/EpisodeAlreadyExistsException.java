package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class EpisodeAlreadyExistsException extends BaseException {

	public EpisodeAlreadyExistsException(UUID seasonId, Integer episodeNumber) {
		super(ErrorCode.EPISODE_ALREADY_EXISTS);
		addDetail("seasonId", seasonId);
		addDetail("episodeNumber", episodeNumber);
	}
}
