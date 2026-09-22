package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class EpisodeNumberChangeBlockedException extends BaseException {

    public EpisodeNumberChangeBlockedException(
            UUID seasonId,
            UUID episodeId,
            Integer currentEpisodeNumber,
            Integer requestedEpisodeNumber
    ) {
        super(ErrorCode.EPISODE_NUMBER_CHANGE_BLOCKED);
        addDetail("seasonId", seasonId);
        addDetail("episodeId", episodeId);
        addDetail("currentEpisodeNumber", currentEpisodeNumber);
        addDetail("requestedEpisodeNumber", requestedEpisodeNumber);
    }
}
