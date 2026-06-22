package com.interview.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.api.model:llama-3.1-8b-instant}")
    private String model;

    public String callAI(String prompt) {

        if (apiKey == null || apiKey.isBlank()) {
            logger.error("Missing Groq API key. Set groq.api.key or GROQ_API_KEY.");
            return "Error calling AI service";
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // message
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        // request body
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(message));
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response =
                    restTemplate.exchange(apiUrl, HttpMethod.POST, request,
                            new ParameterizedTypeReference<Map<String, Object>>() {});

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                logger.error("Null response body from Groq API");
                return "Error calling AI service";
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");

            if (choices == null || choices.isEmpty()) {
                logger.error("No choices in Groq API response");
                return "Error calling AI service";
            }

            Map<String, Object> choice = choices.get(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> messageMap = (Map<String, Object>) choice.get("message");

            if (messageMap == null) {
                logger.error("No message in choice");
                return "Error calling AI service";
            }

            Object content = messageMap.get("content");
            if (content == null) {
                logger.error("No content in Groq API message");
                return "Error calling AI service";
            }

            return content.toString().trim();

        } catch (Exception e) {
            logger.error("Error calling Groq AI API", e);
            return "Error calling AI service";
        }
    }
}
