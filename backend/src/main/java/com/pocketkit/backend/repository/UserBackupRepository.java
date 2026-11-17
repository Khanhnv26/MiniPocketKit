package com.pocketkit.backend.repository;

import com.pocketkit.backend.entity.UserBackupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserBackupRepository extends JpaRepository<UserBackupEntity, Long> {
    Optional<UserBackupEntity> findByUserId(Long userId);
}
