package com.moduplaylist.api.global.security;

import com.moduplaylist.api.auth.service.TemporaryPasswordService;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class TemporaryPasswordAuthenticationProvider extends DaoAuthenticationProvider {

  private static final Logger log = LoggerFactory.getLogger(TemporaryPasswordAuthenticationProvider.class);

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
      UsernamePasswordAuthenticationToken authentication
  ) {
    try {
      super.additionalAuthenticationChecks(userDetails, authentication);
      log.debug("Regular password authentication succeeded");
    } catch (BadCredentialsException exception) {
      if (authentication.getCredentials() == null) {
        throw exception;
      }

      User user = userRepository.findByEmail(userDetails.getUsername())
          .orElseThrow(() -> exception);

      boolean matched = temporaryPasswordService.matches(
          user,
          authentication.getCredentials().toString()
      );

      log.info("Temporary password authentication attempted: matched={}", matched);

      if (!matched) {
        throw exception;
      }
    }
  }
}