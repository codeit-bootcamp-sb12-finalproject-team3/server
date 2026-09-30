package com.moduplaylist.api.user.service.impl;

import com.moduplaylist.api.user.dto.UserCreateRequest;
import com.moduplaylist.api.user.dto.UserProfileResponse;
import com.moduplaylist.api.user.dto.UserProfileUpdateRequest;
import com.moduplaylist.api.user.dto.UserResponse;
import com.moduplaylist.api.user.service.UserService;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.common.exception.ImageStorageUnavailableException;
import com.moduplaylist.core.common.exception.ImageUploadLimitExceededException;
import com.moduplaylist.core.common.exception.InvalidImageException;
import com.moduplaylist.core.common.exception.UnsupportedImageTypeException;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.entity.UserRole;
import com.moduplaylist.core.user.exception.InvalidUserProfileUpdateException;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.exception.UserProfileAccessDeniedException;
import com.moduplaylist.core.user.repository.UserRepository;
import com.moduplaylist.core.user.repository.JwtRegistry;
import com.moduplaylist.core.user.exception.UserAlreadyExistsException;
import com.moduplaylist.infrastructure.storage.UserProfileImageStorage;
import java.io.IOException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtRegistry jwtRegistry;
  private final UserProfileImageStorage userProfileImageStorage;


  private static final Set<String> USER_SORT_FIELDS = Set.of("name", "email", "createdAt", "isLocked", "role");

  @Override
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public CursorPageResponse<UserResponse> findAll(String emailLike, UserRole roleEqual, Boolean isLocked,
      String cursor, UUID idAfter, int limit, String sortBy, SortDirection sortDirection) {
    if (limit < 1 || limit > 100 || sortBy == null || !USER_SORT_FIELDS.contains(sortBy)
        || sortDirection == null || (cursor == null) != (idAfter == null)) {
      throw new BaseException(ErrorCode.INVALID_REQUEST);
    }

    String property = sortBy.equals("isLocked") ? "locked" : sortBy;
    boolean ascending = sortDirection == SortDirection.ASCENDING;
    Object cursorValue = parseUserCursor(cursor, sortBy);

    Specification<User> filters = (root, query, cb) -> {
      Predicate result = cb.conjunction();
      if (emailLike != null && !emailLike.isBlank()) {
        String escaped = emailLike.trim().toLowerCase()
            .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        String pattern = "%" + escaped + "%";
        result = cb.and(result, cb.or(
            cb.like(cb.lower(root.get("email")), pattern, '\\'),
            cb.like(cb.lower(root.get("name")), pattern, '\\')));
      }
      if (roleEqual != null) result = cb.and(result, cb.equal(root.get("role"), roleEqual));
      if (isLocked != null) result = cb.and(result, cb.equal(root.get("locked"), isLocked));
      return result;
    };

    Specification<User> pageFilter = (root, query, cb) -> {
      Predicate filtersPredicate = filters.toPredicate(root, query, cb);
      if (cursorValue == null) return filtersPredicate;

      Predicate afterValue = switch (sortBy) {
        case "name", "email" -> ascending
            ? cb.greaterThan(root.get(property), (String) cursorValue)
            : cb.lessThan(root.get(property), (String) cursorValue);
        case "createdAt" -> ascending
            ? cb.greaterThan(root.get(property), (Instant) cursorValue)
            : cb.lessThan(root.get(property), (Instant) cursorValue);
        case "role" -> ascending
            ? cb.greaterThan(root.get(property), (UserRole) cursorValue)
            : cb.lessThan(root.get(property), (UserRole) cursorValue);
        case "isLocked" -> ascending
            ? cb.and(cb.isTrue(root.get("locked")), cb.isFalse(cb.literal((Boolean) cursorValue)))
            : cb.and(cb.isFalse(root.get("locked")), cb.isTrue(cb.literal((Boolean) cursorValue)));
        default -> throw new BaseException(ErrorCode.INVALID_REQUEST);
      };
      Predicate sameValue = cb.equal(root.get(property), cursorValue);
      Predicate afterId = ascending
          ? cb.greaterThan(root.get("id"), idAfter)
          : cb.lessThan(root.get("id"), idAfter);
      return cb.and(filtersPredicate, cb.or(afterValue, cb.and(sameValue, afterId)));
    };

    Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, property)
        .and(Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, "id"));
    List<User> rows = userRepository.findAll(pageFilter, PageRequest.of(0, limit + 1, sort)).getContent();
    boolean hasNext = rows.size() > limit;
    List<User> visible = hasNext ? rows.subList(0, limit) : rows;
    User last = visible.isEmpty() ? null : visible.get(visible.size() - 1);
    return CursorPageResponse.<UserResponse>builder()
        .data(visible.stream().map(UserResponse::from).toList())
        .nextCursor(hasNext ? cursorFor(last, sortBy) : null)
        .nextIdAfter(hasNext ? last.getId() : null)
        .hasNext(hasNext)
        .totalCount(userRepository.count(filters))
        .sortBy(sortBy)
        .sortDirection(sortDirection)
        .build();
  }

  private Object parseUserCursor(String cursor, String sortBy) {
    if (cursor == null) return null;
    try {
      return switch (sortBy) {
        case "name", "email" -> cursor;
        case "createdAt" -> Instant.parse(cursor);
        case "isLocked" -> {
          if (!cursor.equals("true") && !cursor.equals("false")) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
          }
          yield Boolean.parseBoolean(cursor);
        }
        case "role" -> UserRole.valueOf(cursor);
        default -> throw new BaseException(ErrorCode.INVALID_REQUEST);
      };
    } catch (IllegalArgumentException | DateTimeParseException exception) {
      throw new BaseException(ErrorCode.INVALID_REQUEST);
    }
  }

  private String cursorFor(User user, String sortBy) {
    return switch (sortBy) {
      case "name" -> user.getName();
      case "email" -> user.getEmail();
      case "createdAt" -> user.getCreatedAt().toString();
      case "isLocked" -> Boolean.toString(user.isLocked());
      case "role" -> user.getRole().name();
      default -> throw new BaseException(ErrorCode.INVALID_REQUEST);
    };
  }

  @Override
  @Transactional
  public UserResponse create(UserCreateRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new UserAlreadyExistsException();
    }

    String encodedPassword = passwordEncoder.encode(request.getPassword());

    User user = User.create(
        request.getEmail(),
        encodedPassword,
        request.getName()
    );

    User savedUser = userRepository.save(user);

    return UserResponse.from(savedUser);
  }

  @Override
  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public UserResponse updateRole(UUID userId, UserRole role) {
    if (userId == null || role == null) {
      throw new BaseException(ErrorCode.INVALID_REQUEST);
    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

    user.updateRole(role);

    // 권한 저장이 완료된 뒤 기존 로그인 정보를 삭제한다.
    invalidateAfterCommit(userId);

    return UserResponse.from(user);
  }

  @Override
  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public UserResponse updateLocked(UUID userId, boolean locked) {
    if (userId == null) {
      throw new BaseException(ErrorCode.INVALID_REQUEST);
    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

    user.updateLocked(locked);

    // 계정 잠금 시 기존 로그인을 무효화한다.
    if (locked) {
      invalidateAfterCommit(userId);
    }

    return UserResponse.from(user);
  }

  @Override
  @Transactional(readOnly = true)
  public UserProfileResponse getProfile(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    return UserProfileResponse.from(user);
  }

  @Override
  @Transactional
  public UserProfileResponse updateProfile(
      UUID userId,
      UUID authenticatedUserId,
      UserProfileUpdateRequest request,
      MultipartFile image
  ) {

    if (!userId.equals(authenticatedUserId)) {
      throw new UserProfileAccessDeniedException(userId);
    }

    if (request.getName() == null && image == null) {
      throw new InvalidUserProfileUpdateException();
    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    String previousProfileImageUrl = user.getProfileImageUrl();
    String profileImageUrl = image == null ? null : uploadProfileImage(userId, image);

    user.updateProfile(request.getName(), profileImageUrl);

    if (profileImageUrl != null) {
      registerProfileImageCleanup(userId, previousProfileImageUrl, profileImageUrl);
    }

    return UserProfileResponse.from(user);
  }

  private String uploadProfileImage(UUID userId, MultipartFile image) {
    if (image.isEmpty()) {
      throw new InvalidImageException();
    }
    if (image.getSize() > 5L * 1024 * 1024) {
      throw new ImageUploadLimitExceededException();
    }
    try {
      byte[] bytes = image.getBytes();
      String contentType = detectImageType(bytes);
      if (contentType == null) {
        throw new UnsupportedImageTypeException();
      }
      return userProfileImageStorage.upload(userId, bytes, contentType);
    } catch (IOException exception) {
      throw new ImageStorageUnavailableException(exception);
    } catch (UnsupportedImageTypeException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new ImageStorageUnavailableException(exception);
    }
  }

  private String detectImageType(byte[] bytes) {
    if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
        && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
      return "image/jpeg";
    }
    if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50
        && bytes[2] == 0x4e && bytes[3] == 0x47 && bytes[4] == 0x0d
        && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) {
      return "image/png";
    }
    if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I'
        && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W'
        && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
      return "image/webp";
    }
    return null;
  }

  private void registerProfileImageCleanup(
      UUID userId,
      String previousProfileImageUrl,
      String uploadedProfileImageUrl
  ) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status == STATUS_COMMITTED) {
              deleteProfileImage(userId, previousProfileImageUrl);
            } else {
              deleteProfileImage(userId, uploadedProfileImageUrl);
            }
          }
        }
    );
  }

  private void deleteProfileImage(UUID userId, String profileImageUrl) {
    try {
      userProfileImageStorage.delete(userId, profileImageUrl);
    } catch (RuntimeException exception) {
      log.warn("프로필 이미지 삭제에 실패했습니다. url={}", profileImageUrl, exception);
    }
  }

  private void invalidateAfterCommit(UUID userId) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            jwtRegistry.invalidateByUserId(userId);
          }
        }
    );
  }
}
