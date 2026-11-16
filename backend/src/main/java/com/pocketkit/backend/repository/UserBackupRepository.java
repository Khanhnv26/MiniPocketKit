package com.pocketkit.backend.repository;

import com.pocketkit.backend.entity.UserBackupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserBackupRepository extends JpaRepository<UserBackupEntity, Long> {
    Optional<UserBackupEntity> findByUserId(Long userId);
}
