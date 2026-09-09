package com.moduplaylist.core.user.repository;

import com.moduplaylist.core.user.entity.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, UUID> {

}
