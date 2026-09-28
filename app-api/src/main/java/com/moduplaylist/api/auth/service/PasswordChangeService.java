
package com.moduplaylist.api.auth.service;

import com.moduplaylist.api.auth.dto.PasswordChangeRequest;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.repository.TemporaryPasswordStore;
import com.moduplaylist.core.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class PasswordChangeService {

  private final UserRepository userRepository;
  private final TemporaryPasswordService temporaryPasswordService;
  private final TemporaryPasswordStore temporaryPasswordStore;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public void change(UUID userId, PasswordChangeRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

    if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
      throw new BaseException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
    }

    String currentPassword = request.getCurrentPassword();

    boolean matchesRegularPassword = user.getPassword() != null
        && passwordEncoder.matches(currentPassword, user.getPassword());

    if (!matchesRegularPassword && !temporaryPasswordService.matches(user, currentPassword)) {
      throw new BaseException(ErrorCode.INVALID_CREDENTIALS);
    }

    user.updatePassword(passwordEncoder.encode(request.getNewPassword()));

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            temporaryPasswordStore.delete(userId);
          }
        }
    );
  }
}
