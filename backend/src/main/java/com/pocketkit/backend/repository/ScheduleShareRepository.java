package com.pocketkit.backend.repository;

import com.pocketkit.backend.entity.ScheduleShareEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleShareRepository extends JpaRepository<ScheduleShareEntity, String> {

    List<ScheduleShareEntity> findAllByOwnerUserIdOrderByCreatedAtEpochDesc(Long ownerUserId);
}
