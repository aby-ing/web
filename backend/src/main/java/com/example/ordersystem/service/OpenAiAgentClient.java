package com.example.ordersystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class OpenAiAgentClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiAgentClient.class);

    private final boolean enabled;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final String apiType;
    private final Integer maxOutputTokens;
    private final RestClient client;

    public OpenAiAgentClient(
            @Value("${ai.openai.enabled:true}") boolean enabled,
            @Value("${ai.openai.api-key:${OPENAI_API_KEY:}}") String apiKey,
            @Value("${ai.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.openai.model:${OPENAI_MODEL:gpt-4.1-mini}}") String model,
            @Value("${ai.openai.api-type:chat}") String apiType,
            @Value("${ai.openai.max-output-tokens:500}") Integer maxOutputTokens,
            @Value("${ai.openai.timeout-seconds:8}") Integer timeoutSeconds) {
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.model = model == null ? "" : model.trim();
        this.apiType = apiType == null ? "chat" : apiType.trim().toLowerCase();
        this.maxOutputTokens = maxOutputTokens == null || maxOutputTokens <= 0 ? 500 : maxOutputTokens;
        this.client = RestClient.builder()
                .requestFactory(requestFactory(timeoutSeconds))
                .baseUrl(this.baseUrl)
                .defaultHeader("Authorization", "Bearer " + this.apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public boolean available() {
        return enabled && StringUtils.hasText(apiKey) && StringUtils.hasText(model);
    }

    public Optional<String> ask(String instructions, String input) {
        if (!available()) {
            log.debug("OpenAI agent is disabled or missing api key/model, using local rules.");
            return Optional.empty();
        }
        try {
            JsonNode response = useResponsesApi()
                    ? callResponses(client, instructions, input)
                    : callChatCompletions(client, instructions, input);

            String text = extractText(response);
            if (StringUtils.hasText(text)) {
                return Optional.of(text.trim());
            }
            log.warn("OpenAI agent returned no text, falling back to local rules. Response: {}", summarize(response));
            return Optional.empty();
        } catch (RuntimeException error) {
            log.warn("OpenAI agent request failed, falling back to local rules: {}", error.getMessage());
            return Optional.empty();
        }
    }

    private JsonNode callResponses(RestClient client, String instructions, String input) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", instructions);
        body.put("input", input);
        body.put("max_output_tokens", maxOutputTokens);
        return client.post()
                .uri("/responses")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    private JsonNode callChatCompletions(RestClient client, String instructions, String input) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", instructions),
                Map.of("role", "user", "content", input)
        ));
        body.put("max_tokens", maxOutputTokens);
        body.put("temperature", 0.4);
        body.put("stream", false);
        return client.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    private String extractText(JsonNode response) {
        if (response == null) {
            return "";
        }
        JsonNode choices = response.get("choices");
        if (choices != null && choices.isArray() && !choices.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            JsonNode firstChoice = choices.get(0);
            appendText(builder, firstChoice.path("message").path("content"));
            appendText(builder, firstChoice.path("text"));
            if (!builder.isEmpty()) {
                return builder.toString();
            }
        }
        JsonNode outputText = response.get("output_text");
        if (outputText != null && outputText.isTextual()) {
            return outputText.asText();
        }
        StringBuilder builder = new StringBuilder();
        JsonNode output = response.get("output");
        if (output != null && output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.get("content");
                if (content != null && content.isArray()) {
                    for (JsonNode contentItem : content) {
                        appendText(builder, contentItem);
                    }
                }
                appendText(builder, item.path("text"));
            }
        }
        return builder.toString();
    }

    private void appendText(StringBuilder builder, JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isTextual()) {
            builder.append(node.asText()).append('\n');
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                appendText(builder, item);
            }
            return;
        }
        if (node.isObject()) {
            appendText(builder, node.get("text"));
            appendText(builder, node.get("content"));
            appendText(builder, node.path("text").path("value"));
        }
    }

    private boolean useResponsesApi() {
        return "responses".equals(apiType) || "response".equals(apiType);
    }

    private SimpleClientHttpRequestFactory requestFactory(Integer timeoutSeconds) {
        int seconds = timeoutSeconds == null || timeoutSeconds <= 0 ? 8 : timeoutSeconds;
        int timeoutMillis = seconds * 1000;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        return requestFactory;
    }

    private String summarize(JsonNode response) {
        if (response == null) {
            return "<null>";
        }
        String value = response.toString();
        return value.length() <= 500 ? value : value.substring(0, 500) + "...";
    }

    private String normalizeBaseUrl(String value) {
        String result = value == null ? "https://api.openai.com/v1" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        if (!result.endsWith("/v1")) {
            result = result + "/v1";
        }
        return result;
    }
}
