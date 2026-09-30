package com.moduplaylist.core.user.repository;

import com.moduplaylist.core.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

  boolean existsByEmail(String email);

  Optional<User> findByEmail(String email);

}
