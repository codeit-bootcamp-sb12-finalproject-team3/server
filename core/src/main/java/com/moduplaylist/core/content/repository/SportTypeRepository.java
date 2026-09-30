package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.SportType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SportTypeRepository extends JpaRepository<SportType, UUID> {

	Optional<SportType> findByCode(String code);

	List<SportType> findAllByOrderByNameAsc();
}
