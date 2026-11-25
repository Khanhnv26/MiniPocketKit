package com.pocketkit.backend.repository;

import com.pocketkit.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);
    Optional<UserEntity> findByGoogleSub(String googleSub);
    boolean existsByEmailIgnoreCase(String email);
}
