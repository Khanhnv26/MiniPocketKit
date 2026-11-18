package com.pocketkit.backend.service;

import com.pocketkit.backend.dto.AiDtos;
import org.springframework.web.multipart.MultipartFile;

public interface AiToolsService {

    AiDtos.NoteSummarizeResponse summarizeNote(AiDtos.NoteSummarizeRequest request);
    AiDtos.NoteSummarizeResponse summarizeNoteImage(MultipartFile file);
    AiDtos.ScheduleScanResponse parseScheduleImage(MultipartFile file);
}
