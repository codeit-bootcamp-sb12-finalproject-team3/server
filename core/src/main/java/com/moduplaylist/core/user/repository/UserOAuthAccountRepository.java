package com.moduplaylist.core.user.repository;

import com.moduplaylist.core.user.entity.OAuthProvider;
import com.moduplaylist.core.user.entity.UserOAuthAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOAuthAccountRepository extends JpaRepository<UserOAuthAccount, UUID> {

  Optional<UserOAuthAccount> findByProviderAndProviderUserId(
      OAuthProvider provider,
      String providerUserId
  );

  boolean existsByUser_IdAndProvider(UUID userId, OAuthProvider provider);
}
