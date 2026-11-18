package com.pocketkit.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pocketkit.backend.dto.AiDtos;
import com.pocketkit.backend.service.impl.AiToolsServiceImpl;
import com.pocketkit.backend.service.impl.OcrEngineImpl;
import com.pocketkit.backend.utils.AiClient;
import com.pocketkit.backend.utils.JsonCodec;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiAiToolsTest {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private ObjectMapper objectMapper;
    private ValidatorFactory validatorFactory;
    private MockRestServiceServer mockServer;
    private AiToolsServiceImpl aiToolsService;
    private OcrEngineImpl ocrEngine;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com");
        mockServer = MockRestServiceServer.bindTo(builder).build();

        AiClient aiClient = new AiClient(builder.build(), objectMapper, "test-gemini-key", "gemini-2.5-flash");
        aiToolsService = new AiToolsServiceImpl(aiClient, new JsonCodec(objectMapper), objectMapper, validator);
        ocrEngine = new OcrEngineImpl(aiClient);
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void ocrEngine_callsGeminiWithHeaderAndBase64Image() throws Exception {
        String responseBody = wrapGeminiText("Bài 1: Spring Security & JWT");
        mockServer.expect(requestTo(GEMINI_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-gemini-key"))
                .andExpect(jsonPath("$.contents[0].parts[1].inlineData.mimeType").value("image/png"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        String result = ocrEngine.extractText(new byte[]{1, 2, 3}, "image/png", "slide.png");

        assertEquals("Bài 1: Spring Security & JWT", result);
        mockServer.verify();
    }

    @Test
    void parseScheduleImage_filtersInvalidSlotsAndStripsCodeFence() throws Exception {
        String aiJson = """
                ```json
                {
                  "slots": [
                    {
                      "dayOfWeek": 2,
                      "startTime": "07:30",
                      "endTime": "09:45",
                      "subject": "PRM392",
                      "room": "BE201",
                      "lecturer": "ThS. An",
                      "note": ""
                    },
                    {
                      "dayOfWeek": 9,
                      "startTime": "12:00",
                      "endTime": "09:00",
                      "subject": "Lich loi"
                    }
                  ]
                }
                ```
                """;
        mockServer.expect(requestTo(GEMINI_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andRespond(withSuccess(wrapGeminiText(aiJson), MediaType.APPLICATION_JSON));

        MockMultipartFile image = new MockMultipartFile("file", "tkb.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2});
        AiDtos.ScheduleScanResponse response = aiToolsService.parseScheduleImage(image);

        assertTrue(response.isAiPowered());
        assertEquals(1, response.getSlots().size());
        assertEquals("PRM392", response.getSlots().get(0).getSubject());
        assertEquals(1, response.getSkippedCount());
        mockServer.verify();
    }

    @Test
    void summarizeNote_parsesStructuredJsonAndHandlesUpstreamError() throws Exception {
        String summaryJson = """
                {
                  "title": "Ôn tập JWT",
                  "summary": "JWT giúp xác thực stateless qua Bearer token.",
                  "formattedContent": "- Mã hóa chữ ký HMAC\\n- Không lưu session trên RAM",
                  "keyPoints": ["Stateless", "Hết hạn qua exp"]
                }
                """;
        mockServer.expect(requestTo(GEMINI_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(wrapGeminiText(summaryJson), MediaType.APPLICATION_JSON));

        AiDtos.NoteSummarizeResponse okResponse = aiToolsService.summarizeNote(
                AiDtos.NoteSummarizeRequest.builder().title("JWT").content("Chi tiết về JWT").build()
        );
        assertEquals("Ôn tập JWT", okResponse.getTitle());
        assertEquals(2, okResponse.getKeyPoints().size());

        mockServer.reset();
        mockServer.expect(requestTo(GEMINI_URL))
                .andRespond(withServerError());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> aiToolsService.summarizeNote(
                        AiDtos.NoteSummarizeRequest.builder().title("JWT").content("Chi tiết").build()
                )
        );
        assertEquals(502, ex.getStatusCode().value());
    }

    private String wrapGeminiText(String text) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "candidates",
                new Object[]{
                        Map.of(
                                "content",
                                Map.of("parts", new Object[]{Map.of("text", text)})
                        )
                }
        ));
    }
}
