package com.pocketkit.backend.repository;

import com.pocketkit.backend.entity.ScheduleShareEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleShareRepository extends JpaRepository<ScheduleShareEntity, String> {

}
