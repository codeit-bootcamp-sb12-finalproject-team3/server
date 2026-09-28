package com.moduplaylist.api.global.security;

import com.moduplaylist.api.auth.service.TemporaryPasswordService;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class TemporaryPasswordAuthenticationProvider extends DaoAuthenticationProvider {

  private final UserRepository userRepository;
  private final TemporaryPasswordService temporaryPasswordService;

  public TemporaryPasswordAuthenticationProvider(
      CustomUserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder,
      UserRepository userRepository,
      TemporaryPasswordService temporaryPasswordService
  ) {
    super(userDetailsService);
    setPasswordEncoder(passwordEncoder);
    this.userRepository = userRepository;
    this.temporaryPasswordService = temporaryPasswordService;
  }

  @Override
  protected void additionalAuthenticationChecks(
      UserDetails userDetails,
      org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication
  ) {
    try {
      super.additionalAuthenticationChecks(userDetails, authentication);
    } catch (BadCredentialsException exception) {
      if (authentication.getCredentials() == null) {
        throw exception;
      }

      User user = userRepository.findByEmail(userDetails.getUsername())
          .orElseThrow(() -> exception);

      if (!temporaryPasswordService.matches(user, authentication.getCredentials().toString())) {
        throw exception;
      }
    }
  }
}