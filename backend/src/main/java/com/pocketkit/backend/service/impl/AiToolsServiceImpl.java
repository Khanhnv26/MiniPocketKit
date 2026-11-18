package com.pocketkit.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pocketkit.backend.dto.AiDtos;
import com.pocketkit.backend.dto.BackupDtos;
import com.pocketkit.backend.service.AiToolsService;
import com.pocketkit.backend.utils.AiClient;
import com.pocketkit.backend.utils.ImageFileHelper;
import com.pocketkit.backend.utils.JsonCodec;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class AiToolsServiceImpl implements AiToolsService {

    private static final String NOTE_TEXT_PROMPT_TEMPLATE =
            "Bạn là trợ lý học tập cho sinh viên Việt Nam. Hãy đọc tiêu đề và nội dung ghi chú dưới đây rồi trả về JSON "
                    + "với đúng 4 key: "
                    + "\"title\" (string ngắn gọn), "
                    + "\"summary\" (tóm tắt 1-3 câu tiếng Việt súc tích), "
                    + "\"formattedContent\" (nội dung đã trình bày lại rõ ràng bằng gạch đầu dòng), "
                    + "\"keyPoints\" (mảng string 3-5 ý chính). "
                    + "Tiêu đề hiện tại: %s\nNội dung:\n%s";

    private static final String NOTE_IMAGE_PROMPT =
            "Hãy đọc toàn bộ nội dung trong ảnh ghi chú/slide/bảng viết này rồi trả về JSON với đúng 4 key: "
                    + "\"title\" (tiêu đề phù hợp bằng tiếng Việt), "
                    + "\"summary\" (tóm tắt 1-3 câu tiếng Việt súc tích), "
                    + "\"formattedContent\" (nội dung đầy đủ đã làm sạch và trình bày lại rõ ràng), "
                    + "\"keyPoints\" (mảng string 3-5 ý chính).";

    private static final String SCHEDULE_IMAGE_PROMPT =
            "Hãy trích xuất thời khóa biểu từ ảnh này và trả về JSON có dạng {\"slots\": [...]}. "
                    + "Mỗi phần tử trong \"slots\" phải dùng đúng tên field sau: "
                    + "dayOfWeek (số nguyên 2..7 tương ứng Thứ 2 đến Thứ 7), "
                    + "startTime (định dạng 24h HH:mm, ví dụ 07:30), "
                    + "endTime (định dạng 24h HH:mm sau startTime, ví dụ 09:45), "
                    + "subject (tên môn học, bắt buộc), "
                    + "room (phòng học hoặc chuỗi rỗng), "
                    + "lecturer (tên giảng viên hoặc chuỗi rỗng), "
                    + "note (ghi chú thêm hoặc chuỗi rỗng).";

    private final AiClient aiClient;
    private final JsonCodec jsonCodec;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Override
    public AiDtos.NoteSummarizeResponse summarizeNote(AiDtos.NoteSummarizeRequest request) {
        String title = request.getTitle() != null ? request.getTitle().trim() : "";
        String prompt = String.format(NOTE_TEXT_PROMPT_TEMPLATE, title, request.getContent().trim());
        String json = aiClient.generateJson(prompt, null);
        return parseNoteSummaryResponse(json, title);
    }

    @Override
    public AiDtos.NoteSummarizeResponse summarizeNoteImage(MultipartFile file) {

        ImageFileHelper.ValidatedImage image = ImageFileHelper.validateAndRead(file);
        String json = aiClient.generateJson(NOTE_IMAGE_PROMPT, image);
        return parseNoteSummaryResponse(json, "Ghi chú từ ảnh");
    }

    @Override
    public AiDtos.ScheduleScanResponse parseScheduleImage(MultipartFile file) {

        ImageFileHelper.ValidatedImage image = ImageFileHelper.validateAndRead(file);
        String json = aiClient.generateJson(SCHEDULE_IMAGE_PROMPT, image);

        JsonNode root = jsonCodec.readValue(json, JsonNode.class);
        JsonNode slotsNode = root.isArray() ? root : root.path("slots");

        int skippedCount = 0;
        List<BackupDtos.ScheduleSlotDto> validSlots = new ArrayList<>();

        if(slotsNode.isArray()) {
            for (JsonNode item : slotsNode) {

                try {
                    BackupDtos.ScheduleSlotDto slot = objectMapper.convertValue(item, BackupDtos.ScheduleSlotDto.class);

                    if(slot != null && validator.validate(slot).isEmpty()) {
                        validSlots.add(slot);
                    } else {
                        skippedCount++;
                    }
                } catch (IllegalArgumentException e) {
                    skippedCount++;
                }
            }
        }


        return AiDtos.ScheduleScanResponse.builder()
                .slots(validSlots)
                .skippedCount(skippedCount)
                .aiPowered(true)
                .build();
    }

    private AiDtos.NoteSummarizeResponse parseNoteSummaryResponse(String json, String fallbackTitle) {
        JsonNode root = jsonCodec.readValue(json, JsonNode.class);
        String title = root.path("title").asText("").trim();
        String summary = root.path("summary").asText("").trim();
        String formattedContent = root.path("formattedContent").asText("").trim();

        List<String> keyPoints = new ArrayList<>();

        JsonNode keyPointsNode = root.path("keyPoints");
        if (keyPointsNode.isArray()) {
            for(JsonNode item : keyPointsNode) {
                String point = item.asText("").trim();
                if(!point.isEmpty()) {
                    keyPoints.add(point);
                }
            }
        }

        return AiDtos.NoteSummarizeResponse.builder()
                .title(!title.isEmpty() ? title : fallbackTitle)
                .summary(summary)
                .formattedContent(formattedContent)
                .keyPoints(keyPoints)
                .aiPowered(true)
                .build();
    }
}
