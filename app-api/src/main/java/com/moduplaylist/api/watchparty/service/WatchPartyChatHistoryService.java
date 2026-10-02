package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.watchparty.dto.WatchPartyChatMessageResponse;
import com.moduplaylist.api.user.dto.UserSummary;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.exception.WatchPartyChatAccessDeniedException;
import com.moduplaylist.core.watchparty.exception.WatchPartyNotFoundException;
import com.moduplaylist.core.watchparty.repository.WatchPartyChatLogRegistry;
import com.moduplaylist.core.watchparty.repository.WatchPartyParticipantRepository;
import com.moduplaylist.core.watchparty.repository.WatchPartyRepository;
import com.moduplaylist.core.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchPartyChatHistoryService {

    static final int MAX_LIMIT = 500; // Redis chat:log 보관 상한과 동일

    private final WatchPartyRepository watchPartyRepository;
    private final WatchPartyParticipantRepository watchPartyParticipantRepository;
    private final WatchPartyChatLogRegistry watchPartyChatLogRegistry;
    private final UserRepository userRepository;

    public List<WatchPartyChatMessageResponse> getRecentMessages(UUID partyId, UUID userId, int limit) {
        validateLimit(limit);

        if (!watchPartyRepository.existsById(partyId)) {
            throw new WatchPartyNotFoundException(partyId);
        }

        if (isKicked(partyId, userId)) {
            throw new WatchPartyChatAccessDeniedException(partyId, userId);
        }

        var messages = watchPartyChatLogRegistry.findRecent(partyId, limit);
        var senderIds = messages.stream()
                .map(message -> message.getSenderId())
                .distinct()
                .toList();
        Map<UUID, UserSummary> sendersById = userRepository.findAllById(senderIds).stream()
                .map(UserSummary::from)
                .collect(Collectors.toMap(UserSummary::getUserId, Function.identity()));

        return messages.stream()
                .map(message -> WatchPartyChatMessageResponse.from(
                        message,
                        sendersById.get(message.getSenderId())))
                .toList();
    }

    // 참가 기록이 없는 구경꾼은 false → 조회 허용
    private boolean isKicked(UUID partyId, UUID userId) {
        return watchPartyParticipantRepository
                .findByUser_IdAndWatchParty_Id(userId, partyId)
                .map(participant -> participant.getStatus() == ParticipantStatus.KICKED)
                .orElse(false);
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }
    }
}
