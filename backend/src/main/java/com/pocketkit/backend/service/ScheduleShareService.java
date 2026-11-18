package com.pocketkit.backend.service;


import com.pocketkit.backend.dto.ScheduleShareDtos;

public interface ScheduleShareService {

    ScheduleShareDtos.CreateShareResponse createShare(Long userId, ScheduleShareDtos.CreateShareRequest request);
    ScheduleShareDtos.ShareDetailResponse getByCode(String rawCode);


}
