package com.kkalchake.enlightenment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiProviderTest {

    private static final String BASE_URL = "http://test-host";

    private MockRestServiceServer server;
    private GeminiProvider geminiProvider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        geminiProvider = new GeminiProvider(builder, objectMapper, "test-api-key", "gemini-test", BASE_URL);
    }

    @Test
    void chat_success() {
        String jsonResponse = """
                {
                    "candidates": [
                        {
                            "content": {
                                "parts": [
                                    {"text": "Hello from Gemini!"}
                                ]
                            }
                        }
                    ]
                }
                """;

        server.expect(requestTo(containsString("test-api-key")))
                .andExpect(requestTo(containsString("gemini-test")))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        String result = geminiProvider.chat("Hello");

        assertEquals("Hello from Gemini!", result);
        server.verify();
    }

    @Test
    void chat_emptyApiKey_throwsException() {
        RestClient.Builder builder = RestClient.builder();
        GeminiProvider providerWithEmptyKey = new GeminiProvider(builder, objectMapper, "", "gemini-test", BASE_URL);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> providerWithEmptyKey.chat("Hello"));
        assertEquals("AI service is not properly configured", exception.getMessage());
    }

    @Test
    void chat_nullApiKey_throwsException() {
        RestClient.Builder builder = RestClient.builder();
        GeminiProvider providerWithNullKey = new GeminiProvider(builder, objectMapper, null, "gemini-test", BASE_URL);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> providerWithNullKey.chat("Hello"));
        assertEquals("AI service is not properly configured", exception.getMessage());
    }

    @Test
    void chat_apiError_throwsException() {
        server.expect(requestTo(containsString("/v1beta/models/")))
                .andRespond(withServerError());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> geminiProvider.chat("Hello"));
        assertTrue(exception.getMessage().contains("Failed to get response"));
        server.verify();
    }

    @Test
    void chat_emptyCandidates_returnsDefaultMessage() {
        String jsonResponse = """
                {
                    "candidates": []
                }
                """;

        server.expect(requestTo(containsString("/v1beta/models/")))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        String result = geminiProvider.chat("Hello");

        assertEquals("No response generated", result);
        server.verify();
    }

    @Test
    void chat_missingParts_returnsDefaultMessage() {
        String jsonResponse = """
                {
                    "candidates": [
                        {
                            "content": {}
                        }
                    ]
                }
                """;

        server.expect(requestTo(containsString("/v1beta/models/")))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        String result = geminiProvider.chat("Hello");

        assertEquals("No response generated", result);
        server.verify();
    }

    @Test
    void getModelName_returnsConfiguredModel() {
        assertEquals("gemini-test", geminiProvider.getModelName());
    }
}
