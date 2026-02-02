package com.example.project.global.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final WebClient geminiWebClient;
    private final GeminiProperties props;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateText(String prompt) {
        String model = props.model();
        String path = "/v1beta/models/" + model + ":generateContent";

        try {
            var body = objectMapper.createObjectNode();
            var contents = body.putArray("contents");
            var c0 = contents.addObject();
            var parts = c0.putArray("parts");
            parts.addObject().put("text", prompt);

            String raw = geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path(path)
                    .queryParam("key", props.apiKey())
                    .build()
                )
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                    resp.bodyToMono(String.class).map(errBody ->
                        new RuntimeException("Gemini HTTP error: " + resp.statusCode() + " body=" + errBody)
                    )
                )
                .bodyToMono(String.class)
                .block();

            if (raw == null || raw.isBlank()) {
                throw new IllegalStateException("Gemini response empty");
            }

            JsonNode root = objectMapper.readTree(raw);
            JsonNode textNode = root.at("/candidates/0/content/parts/0/text");
            if (textNode.isMissingNode()) {
                throw new IllegalStateException("Gemini response missing candidates text. raw=" + raw);
            }

            return textNode.asText().trim();

        } catch (WebClientResponseException e) {
            throw new RuntimeException("Gemini HTTP exception: status=" + e.getStatusCode()
                + " body=" + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Gemini generateText failed: " + e.getMessage(), e);
        }
    }

    /**
     * Gemini가 JSON 외 텍스트를 섞어 보내도, 첫 번째 {...} JSON 오브젝트만 뽑아내는 유틸.
     */
    public String extractJsonObject(String s) {
        if (s == null) return null;

        String t = s.replace("```json", "").replace("```", "").trim();

        int start = t.indexOf('{');
        if (start < 0) throw new IllegalStateException("No JSON object start found. text=" + t);

        int depth = 0;
        for (int i = start; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '{') depth++;
            if (c == '}') depth--;
            if (depth == 0) return t.substring(start, i + 1).trim();
        }
        throw new IllegalStateException("No JSON object end found. text=" + t);
    }
}
