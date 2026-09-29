package com.moduplaylist.api.auth.service;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.repository.TemporaryPasswordStore;
import com.moduplaylist.core.user.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TemporaryPasswordService {

  private static final Duration EXPIRATION = Duration.ofMinutes(3);
  private static final String CHARACTERS =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
  private static final int PASSWORD_LENGTH = 12;

  private final UserRepository userRepository;
  private final TemporaryPasswordStore temporaryPasswordStore;
  private final PasswordEncoder passwordEncoder;
  private final TemporaryPasswordMailService temporaryPasswordMailService;

  private final SecureRandom secureRandom = new SecureRandom();

  public String issue(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

    if (user.getPassword() == null) {
      throw new BaseException(ErrorCode.INVALID_REQUEST);
    }

    String temporaryPassword = generatePassword();
    String encodedPassword = passwordEncoder.encode(temporaryPassword);

    temporaryPasswordStore.save(user.getId(), encodedPassword, EXPIRATION);

    try {
      temporaryPasswordMailService.send(email, temporaryPassword);
    } catch (MailException e) {
      temporaryPasswordStore.delete(user.getId());
      throw e;
    }

    return temporaryPassword;
  }

  public boolean matches(User user, String rawPassword) {
    String encodedPassword = temporaryPasswordStore.findByUserId(user.getId());

    return encodedPassword != null
        && passwordEncoder.matches(rawPassword, encodedPassword);
  }

  public void delete(User user) {
    temporaryPasswordStore.delete(user.getId());
  }

  private String generatePassword() {
    StringBuilder password = new StringBuilder(PASSWORD_LENGTH);

    for (int i = 0; i < PASSWORD_LENGTH; i++) {
      int index = secureRandom.nextInt(CHARACTERS.length());
      password.append(CHARACTERS.charAt(index));
    }

    return password.toString();
  }
}
