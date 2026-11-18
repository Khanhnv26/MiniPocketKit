package com.pocketkit.backend.service;


import com.pocketkit.backend.dto.ScheduleShareDtos;

import java.util.List;

public interface ScheduleShareService {

    ScheduleShareDtos.CreateShareResponse createShare(Long userId, ScheduleShareDtos.CreateShareRequest request);
    ScheduleShareDtos.ShareDetailResponse getByCode(String rawCode);
    List<ScheduleShareDtos.ShareSummaryResponse> listMyShares(Long userId);
    void deleteShare(Long userId, String rawCode);

}
