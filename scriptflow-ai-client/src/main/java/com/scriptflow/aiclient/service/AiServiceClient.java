package com.scriptflow.aiclient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.framework.properties.AiServiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for the Python AI microservice.
 * Handles communication with the LangGraph-based multi-agent pipeline.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceClient {

    private final OkHttpClient httpClient;
    private final AiServiceProperties aiProperties;
    private final ObjectMapper objectMapper;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * Submit an AI processing task.
     *
     * @param taskId    internal task ID
     * @param taskType  task type (novel_analysis, character_extract, script_generate, etc.)
     * @param params    task parameters as JSON string
     * @return response from AI service
     */
    public Map<String, Object> submitTask(Long taskId, String taskType, String params) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("task_id", taskId);
            requestBody.put("task_type", taskType);
            requestBody.put("params", params);
            requestBody.put("callback_url", aiProperties.getBaseUrl() + "/api/v1/tasks/" + taskId + "/callback");

            String json = objectMapper.writeValueAsString(requestBody);

            Request request = new Request.Builder()
                    .url(aiProperties.getBaseUrl() + "/api/v1/tasks")
                    .post(RequestBody.create(json, JSON))
                    .addHeader("Authorization", "Bearer " + aiProperties.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("AI service returned error: {} {}", response.code(), response.body());
                    throw new BusinessException(ResultCode.AI_SERVICE_ERROR,
                            "AI service error: HTTP " + response.code());
                }
                String responseBody = response.body() != null ? response.body().string() : "{}";
                return objectMapper.readValue(responseBody, Map.class);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call AI service", e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI service unavailable: " + e.getMessage());
        }
    }

    /**
     * Query task status from AI service.
     */
    public Map<String, Object> getTaskStatus(Long taskId) {
        try {
            Request request = new Request.Builder()
                    .url(aiProperties.getBaseUrl() + "/api/v1/tasks/" + taskId)
                    .get()
                    .addHeader("Authorization", "Bearer " + aiProperties.getApiKey())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new BusinessException(ResultCode.AI_SERVICE_ERROR,
                            "Failed to query task status: HTTP " + response.code());
                }
                String responseBody = response.body() != null ? response.body().string() : "{}";
                return objectMapper.readValue(responseBody, Map.class);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to query AI task status: {}", taskId, e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI service unavailable");
        }
    }
}
