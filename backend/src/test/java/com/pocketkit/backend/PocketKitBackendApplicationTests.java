package com.pocketkit.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PocketKitBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthEndpoint_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("UP")));
    }

    @Test
    void authBackupShareAndOcr_endToEndContractWorks() throws Exception {
        String registerBody = """
                {
                  "email": "khanhnv@pocketkit.vn",
                  "password": "secret123",
                  "fullName": "Nguyễn Khánh"
                }
                """;

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is("khanhnv@pocketkit.vn")))
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String token = registerJson.get("data").get("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));

        mockMvc.perform(get("/api/v1/backup/latest"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));

        String backupBody = """
                {
                  "simulatedScoreProgress": 90,
                  "profile": {
                    "fullName": "Nguyễn Khánh",
                    "majorAndCohort": "Kỹ thuật Phần mềm · K19",
                    "earnedCredits": 114,
                    "totalCredits": 145,
                    "attendancePercent": 96,
                    "baselineGpa": 3.44,
                    "currentTermCredits": 20
                  },
                  "categories": ["PRM392", "SWP391"],
                  "schedules": [
                    {
                      "dayOfWeek": 2,
                      "startTime": "07:30",
                      "endTime": "09:45",
                      "subject": "PRM392",
                      "room": "BE201",
                      "lecturer": "ThS. An",
                      "note": "Mang laptop"
                    }
                  ],
                  "tasks": [
                    {
                      "title": "Nộp Lab 04",
                      "subject": "PRM392",
                      "tag": "Deadline",
                      "dueAtMillis": 1794819600000,
                      "done": false,
                      "createdAtMillis": 1794700000000,
                      "remindBeforeMinutes": 60
                    }
                  ],
                  "notes": [
                    {
                      "title": "Ôn thi PRM392",
                      "content": "SQLite & MVVM",
                      "subject": "PRM392",
                      "isPinned": 1,
                      "isDone": 0,
                      "updatedAt": "16/11 19:00"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/backup")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(backupBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.payload.profile.fullName", is("Nguyễn Khánh")));

        mockMvc.perform(get("/api/v1/backup/latest")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payload.schedules", hasSize(1)))
                .andExpect(jsonPath("$.data.payload.tasks[0].remindBeforeMinutes", is(60)))
                .andExpect(jsonPath("$.data.payload.notes[0].isPinned", is(1)));

        String shareBody = """
                {
                  "title": "SE1902 Kỳ Fall",
                  "slots": [
                    {
                      "dayOfWeek": 3,
                      "startTime": "09:50",
                      "endTime": "12:00",
                      "subject": "SWP391",
                      "room": "DE302",
                      "lecturer": "Cô Bình",
                      "note": ""
                    }
                  ]
                }
                """;

        MvcResult shareResult = mockMvc.perform(post("/api/v1/schedules/share")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shareBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code", notNullValue()))
                .andReturn();

        String code = objectMapper.readTree(shareResult.getResponse().getContentAsString())
                .get("data").get("code").asText();

        mockMvc.perform(get("/api/v1/schedules/share/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title", is("SE1902 Kỳ Fall")))
                .andExpect(jsonPath("$.data.slots[0].subject", is("SWP391")));

        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "slide.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/v1/tools/ocr")
                        .file(imageFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.language", is("vie+eng")));

        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "bad.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "invalid".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/tools/ocr")
                        .file(textFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
