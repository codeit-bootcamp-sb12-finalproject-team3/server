package com.moduplaylist.api.global.security;

import java.util.UUID;
import com.moduplaylist.api.user.dto.UserResponse;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;


  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

    User user = userRepository.findByEmail(email)
        .orElseThrow(() ->
            new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

    return new CustomUserDetails(
        UserResponse.from(user),
        user.getPassword()
    );
  }

  @Transactional(readOnly = true)
  public UserDetails loadUserById(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() ->
            new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

    return new CustomUserDetails(
        UserResponse.from(user),
        user.getPassword()
    );
  }
}
