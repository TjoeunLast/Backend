package com.example.project.domain.payment.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TossPaymentHttpClient implements TossPaymentClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${payment.toss.base-url:https://api.tosspayments.com}")
    private String tossBaseUrl;

    @Value("${payment.toss.secret-key:}")
    private String tossSecretKey;

    @Override
    public ConfirmResult confirm(String paymentKey, String pgOrderId, BigDecimal amount) {
        if (tossSecretKey == null || tossSecretKey.isBlank()) {
            String mockTid = "MOCK-TOSS-" + UUID.randomUUID();
            String raw = "{\"mock\":true,\"orderId\":\"" + pgOrderId + "\",\"paymentKey\":\"" + paymentKey + "\"}";
            return new ConfirmResult(true, mockTid, null, null, raw);
        }

        String auth = Base64.getEncoder().encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));

        String responseBody;
        try {
            responseBody = webClientBuilder.baseUrl(tossBaseUrl).build()
                    .post()
                    .uri("/v1/payments/confirm")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new ConfirmBody(paymentKey, pgOrderId, amount))
                    .retrieve()
                    .onStatus(
                            status -> status.value() >= 400,
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new IllegalStateException(body)))
                    )
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            String message = e.getMessage();
            return new ConfirmResult(false, null, "TOSS_HTTP_ERROR", message, message);
        }

        try {
            JsonNode node = objectMapper.readTree(responseBody);
            String txId = readText(node, "paymentKey");
            if (txId == null) {
                txId = readText(node, "mId");
            }
            if (txId == null) {
                txId = "TOSS-" + UUID.randomUUID();
            }
            return new ConfirmResult(true, txId, null, null, responseBody);
        } catch (Exception e) {
            return new ConfirmResult(false, null, "TOSS_PARSE_ERROR", e.getMessage(), responseBody);
        }
    }

    private String readText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private record ConfirmBody(String paymentKey, String orderId, BigDecimal amount) {}
}

