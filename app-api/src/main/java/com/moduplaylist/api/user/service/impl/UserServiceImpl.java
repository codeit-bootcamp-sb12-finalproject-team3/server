package com.moduplaylist.api.user.service.impl;

import com.moduplaylist.api.user.dto.UserCreateRequest;
import com.moduplaylist.api.user.dto.UserResponse;
import com.moduplaylist.api.user.service.UserService;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.entity.UserRole;
import com.moduplaylist.core.user.repository.UserRepository;
import com.moduplaylist.core.user.repository.JwtRegistry;
import com.moduplaylist.core.user.exception.UserAlreadyExistsException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtRegistry jwtRegistry;


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
