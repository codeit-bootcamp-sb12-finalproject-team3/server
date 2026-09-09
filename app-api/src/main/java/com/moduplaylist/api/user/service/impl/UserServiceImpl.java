package com.moduplaylist.api.user.service.impl;

import com.moduplaylist.api.user.dto.UserCreateRequest;
import com.moduplaylist.api.user.dto.UserResponse;
import com.moduplaylist.api.user.service.UserService;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.UserAlreadyExistsException;
import com.moduplaylist.core.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;


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
}
