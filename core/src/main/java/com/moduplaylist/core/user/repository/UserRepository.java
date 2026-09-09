package com.moduplaylist.core.user.repository;

import com.moduplaylist.core.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

}
