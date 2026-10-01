package com.moduplaylist.api.user.config;

import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.entity.UserRole;
import com.moduplaylist.core.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAccountInitializer implements ApplicationRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${mopl.admin.email:}")
  private String adminEmail;

  @Value("${mopl.admin.password:}")
  private String adminPassword;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (adminEmail == null || adminEmail.isBlank()
        || adminPassword == null || adminPassword.isBlank()) {
      throw new IllegalStateException("초기 관리자 계정 환경변수가 설정되지 않았습니다.");
    }

    String email = adminEmail.trim();

    if (userRepository.existsByEmail(email)) {
      log.info("초기 관리자 이메일에 해당하는 기존 계정이 있어 생성을 건너뜁니다.");
      return;
    }

    User admin = User.create(email, passwordEncoder.encode(adminPassword), "관리자");
    admin.updateRole(UserRole.ADMIN);

    userRepository.save(admin);
    log.info("초기 관리자 계정을 생성했습니다.");
  }
}
