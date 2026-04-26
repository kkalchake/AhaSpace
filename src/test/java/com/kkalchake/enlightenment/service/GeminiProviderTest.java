package com.kkalchake.enlightenment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

class GeminiProviderTest {

    private MockWebServer mockWebServer;
    private GeminiProvider geminiProvider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockWebServer = new MockWebServer();
        objectMapper = new ObjectMapper();
        String baseUrl = mockWebServer.url("/").toString();
        WebClient.Builder webClientBuilder = WebClient.builder();
        geminiProvider = new GeminiProvider(webClientBuilder, objectMapper, "test-api-key", "gemini-test", baseUrl);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void chat_success() throws Exception {
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

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        String result = geminiProvider.chat("Hello");

        assertEquals("Hello from Gemini!", result);

        RecordedRequest request = mockWebServer.takeRequest();
        assertTrue(request.getPath().contains("test-api-key"));
        assertTrue(request.getPath().contains("gemini-test"));
    }

    @Test
    void chat_emptyApiKey_throwsException() {
        String baseUrl = mockWebServer.url("/").toString();
        WebClient.Builder webClientBuilder = WebClient.builder();
        GeminiProvider providerWithEmptyKey = new GeminiProvider(webClientBuilder, objectMapper, "", "gemini-test", baseUrl);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> providerWithEmptyKey.chat("Hello"));
        assertEquals("AI service is not properly configured", exception.getMessage());
    }

    @Test
    void chat_nullApiKey_throwsException() {
        String baseUrl = mockWebServer.url("/").toString();
        WebClient.Builder webClientBuilder = WebClient.builder();
        GeminiProvider providerWithNullKey = new GeminiProvider(webClientBuilder, objectMapper, null, "gemini-test", baseUrl);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> providerWithNullKey.chat("Hello"));
        assertEquals("AI service is not properly configured", exception.getMessage());
    }

    @Test
    void chat_apiError_throwsException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> geminiProvider.chat("Hello"));
        assertTrue(exception.getMessage().contains("Failed to get response"));
    }

    @Test
    void chat_emptyCandidates_returnsDefaultMessage() {
        String jsonResponse = """
                {
                    "candidates": []
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        String result = geminiProvider.chat("Hello");

        assertEquals("No response generated", result);
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

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        String result = geminiProvider.chat("Hello");

        assertEquals("No response generated", result);
    }

    @Test
    void getModelName_returnsConfiguredModel() {
        assertEquals("gemini-test", geminiProvider.getModelName());
    }
}
