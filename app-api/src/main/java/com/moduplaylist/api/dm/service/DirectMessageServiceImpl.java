package com.moduplaylist.api.dm.service;

import com.moduplaylist.api.dm.dto.ConversationListResponse;
import com.moduplaylist.api.dm.dto.ConversationResponse;
import com.moduplaylist.api.dm.dto.ConversationSearchRequest;
import com.moduplaylist.api.dm.dto.DirectMessageCreateResult;
import com.moduplaylist.api.dm.dto.DirectMessageResponse;
import com.moduplaylist.api.dm.dto.DirectMessageSearchRequest;
import com.moduplaylist.api.dm.event.DmMessageCreatedEvent;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.dm.entity.Conversation;
import com.moduplaylist.core.dm.entity.ConversationParticipant;
import com.moduplaylist.core.dm.entity.DirectMessage;
import com.moduplaylist.core.dm.exception.ConversationAccessDeniedException;
import com.moduplaylist.core.dm.exception.ConversationNotFoundException;
import com.moduplaylist.core.dm.exception.DirectMessageNotFoundException;
import com.moduplaylist.core.dm.exception.InvalidDirectMessageContentException;
import com.moduplaylist.core.dm.exception.SelfDirectMessageNotAllowedException;
import com.moduplaylist.core.dm.repository.ConversationParticipantRepository;
import com.moduplaylist.core.dm.repository.ConversationListItem;
import com.moduplaylist.core.dm.repository.ConversationRepository;
import com.moduplaylist.core.dm.repository.DirectMessageRepository;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.repository.UserRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DirectMessageServiceImpl implements DirectMessageService {

    private static final String CONVERSATION_SORT_BY = "updatedAt,id";
    private static final String MESSAGE_SORT_BY = "createdAt,id";

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public DirectMessageCreateResult createMessage(
            UUID conversationId,
            UUID senderId,
            String content
    ) {
        if (conversationId == null) {
            throw new ConversationNotFoundException(null);
        }
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        if (senderId == null) {
            throw new ConversationAccessDeniedException(conversationId);
        }
        ConversationParticipant senderParticipant = participantRepository
                .findByConversation_IdAndUser_Id(conversationId, senderId)
                .orElseThrow(() -> new ConversationAccessDeniedException(conversationId));
        List<User> peers = participantRepository.findPeers(conversationId, senderId);
        if (peers.size() != 1) {
            throw new ConversationNotFoundException(conversationId);
        }
        User receiver = peers.get(0);

        validateMessageContent(content);

        DirectMessage savedMessage = directMessageRepository.saveAndFlush(
                DirectMessage.create(conversation, senderParticipant.getUser(), content)
        );
        DirectMessageCreateResult result = new DirectMessageCreateResult(
                savedMessage.getId(),
                conversationId,
                senderId,
                receiver.getId(),
                savedMessage.getContent(),
                savedMessage.getCreatedAt()
        );

        conversationRepository.updateUpdatedAtIfOlder(
                conversationId,
                savedMessage.getCreatedAt()
        );
        eventPublisher.publishEvent(new DmMessageCreatedEvent(
                result.getMessageId(),
                result.getConversationId(),
                result.getSenderId(),
                result.getReceiverId(),
                result.getContent(),
                result.getCreatedAt()
        ));
        return result;
    }

    @Override
    public ConversationResponse createOrGetConversation(UUID userId, UUID peerId) {
        validateAuthenticatedUser(userId);
        if (peerId == null) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }
        if (userId.equals(peerId)) {
            throw new SelfDirectMessageNotAllowedException(userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.UNAUTHORIZED));
        User peer = userRepository.findById(peerId)
                .orElseThrow(() -> new UserNotFoundException(peerId));

        List<Conversation> existing = participantRepository
                .findDirectConversationsBetween(userId, peerId);
        if (!existing.isEmpty()) {
            return toConversationResponse(existing.get(0), userId);
        }

        Conversation conversation = conversationRepository.save(Conversation.create());
        participantRepository.saveAll(List.of(
                ConversationParticipant.create(conversation, user),
                ConversationParticipant.create(conversation, peer)
        ));

        return ConversationResponse.from(conversation, peer, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<ConversationListResponse> getConversations(
            UUID userId,
            ConversationSearchRequest request
    ) {
        validateAuthenticatedUser(userId);
        if (request == null) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }
        validateCursorRequest(
                request.getCursor(),
                request.getIdAfter(),
                request.getLimit()
        );

        Instant cursor = parseCursor(request.getCursor());
        UUID idAfter = parseIdAfter(request.getIdAfter());
        Pageable pageable = PageRequest.of(0, request.getLimit());
        Slice<ConversationListItem> slice = participantRepository.findConversationPage(
                userId,
                cursor,
                idAfter,
                pageable
        );

        List<ConversationListResponse> data = slice.getContent().stream()
                .map(item -> ConversationListResponse.from(
                        item.getConversation(),
                        item.getPeer(),
                        item.getLatestMessage(),
                        Boolean.TRUE.equals(item.getHasUnread())
                ))
                .toList();

        Conversation last = slice.hasNext() && !slice.getContent().isEmpty()
                ? slice.getContent().get(slice.getContent().size() - 1).getConversation()
                : null;

        return CursorPageResponse.<ConversationListResponse>builder()
                .data(data)
                .nextCursor(last == null ? null : last.getUpdatedAt().toString())
                .nextIdAfter(last == null ? null : last.getId())
                .hasNext(slice.hasNext())
                .totalCount(participantRepository.countByUser_Id(userId))
                .sortBy(CONVERSATION_SORT_BY)
                .sortDirection(SortDirection.DESCENDING)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID userId, UUID conversationId) {
        validateAuthenticatedUser(userId);
        if (conversationId == null) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        if (!participantRepository.existsByConversation_IdAndUser_Id(
                conversationId,
                userId
        )) {
            throw new ConversationAccessDeniedException(conversationId);
        }

        List<User> peers = participantRepository.findPeers(conversationId, userId);
        if (peers.size() != 1) {
            throw new ConversationNotFoundException(conversationId);
        }

        DirectMessage latestMessage = directMessageRepository
                .findFirstByConversation_IdOrderByCreatedAtDescIdDesc(conversationId)
                .orElse(null);

        return ConversationResponse.from(conversation, peers.get(0), latestMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<DirectMessageResponse> getMessages(
            UUID userId,
            UUID conversationId,
            DirectMessageSearchRequest request
    ) {
        validateAuthenticatedUser(userId);
        validateConversationAccess(userId, conversationId);
        validateSearchRequest(request);

        Instant cursor = parseCursor(request.getCursor());
        UUID idAfter = parseIdAfter(request.getIdAfter());
        Pageable pageable = PageRequest.of(0, request.getLimit());

        Slice<DirectMessage> slice = cursor == null
                ? directMessageRepository
                .findByConversation_IdOrderByCreatedAtDescIdDesc(conversationId, pageable)
                : directMessageRepository
                .findNextPage(conversationId, cursor, idAfter, pageable);

        List<DirectMessageResponse> data = slice.getContent().stream()
                .map(DirectMessageResponse::from)
                .toList();

        DirectMessage last = slice.hasNext() && !slice.getContent().isEmpty()
                ? slice.getContent().get(slice.getContent().size() - 1)
                : null;

        return CursorPageResponse.<DirectMessageResponse>builder()
                .data(data)
                .nextCursor(last == null ? null : last.getCreatedAt().toString())
                .nextIdAfter(last == null ? null : last.getId())
                .hasNext(slice.hasNext())
                .totalCount(directMessageRepository.countByConversation_Id(conversationId))
                .sortBy(MESSAGE_SORT_BY)
                .sortDirection(SortDirection.DESCENDING)
                .build();
    }

    @Override
    public void markAsRead(UUID userId, UUID conversationId, UUID lastReadMessageId) {
        validateAuthenticatedUser(userId);
        validateConversationAccess(userId, conversationId);
        if (lastReadMessageId == null) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }

        DirectMessage lastReadMessage = directMessageRepository
                .findByIdAndConversation_Id(lastReadMessageId, conversationId)
                .orElseThrow(() -> new DirectMessageNotFoundException(lastReadMessageId));

        directMessageRepository.markMessagesAsRead(
                conversationId,
                userId,
                lastReadMessage.getCreatedAt(),
                lastReadMessage.getId(),
                Instant.now()
        );
    }

    private ConversationResponse toConversationResponse(
            Conversation conversation,
            UUID currentUserId
    ) {
        User peer = participantRepository.findByConversation_Id(conversation.getId()).stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !user.getId().equals(currentUserId))
                .findFirst()
                .orElseThrow(() -> new ConversationNotFoundException(conversation.getId()));

        DirectMessage latestMessage = directMessageRepository
                .findFirstByConversation_IdOrderByCreatedAtDescIdDesc(conversation.getId())
                .orElse(null);

        return ConversationResponse.from(conversation, peer, latestMessage);
    }

    private void validateConversationAccess(UUID userId, UUID conversationId) {
        if (conversationId == null) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }
        if (!conversationRepository.existsById(conversationId)) {
            throw new ConversationNotFoundException(conversationId);
        }
        if (!participantRepository.existsByConversation_IdAndUser_Id(conversationId, userId)) {
            throw new ConversationAccessDeniedException(conversationId);
        }
    }

    private void validateAuthenticatedUser(UUID userId) {
        if (userId == null) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void validateMessageContent(String content) {
        if (content == null || content.isBlank() || content.length() > 255) {
            throw new InvalidDirectMessageContentException();
        }
    }

    private void validateSearchRequest(DirectMessageSearchRequest request) {
        validateCursorRequest(
                request == null ? null : request.getCursor(),
                request == null ? null : request.getIdAfter(),
                request == null ? 0 : request.getLimit()
        );
    }

    private void validateCursorRequest(String cursor, String idAfter, int limit) {
        if (limit < 1 || limit > 100) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }
        boolean cursorEmpty = cursor == null || cursor.isBlank();
        boolean idAfterEmpty = idAfter == null || idAfter.isBlank();
        if (cursorEmpty != idAfterEmpty) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Instant parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(cursor);
        } catch (DateTimeParseException exception) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, exception);
        }
    }

    private UUID parseIdAfter(String idAfter) {
        if (idAfter == null || idAfter.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(idAfter);
        } catch (IllegalArgumentException exception) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, exception);
        }
    }
}
