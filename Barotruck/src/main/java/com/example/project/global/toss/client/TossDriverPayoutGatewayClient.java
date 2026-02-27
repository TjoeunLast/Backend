package com.example.project.global.toss.client;

import com.example.project.domain.payment.service.client.DriverPayoutGatewayClient;
import com.example.project.member.domain.Driver;
import com.example.project.member.repository.DriverRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.payout.mock-enabled", havingValue = "false")
public class TossDriverPayoutGatewayClient implements DriverPayoutGatewayClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final DriverRepository driverRepository;

    @Value("${payment.toss.base-url:https://api.tosspayments.com}")
    private String tossBaseUrl;

    @Value("${payment.toss.payout-path:/v1/payouts}")
    private String payoutPath;

    @Value("${payment.toss.payout.secret-key:${payment.toss.secret-key:}}")
    private String payoutSecretKey;

    @Override
    public PayoutResult payout(Long orderId, Long driverUserId, BigDecimal netAmount, Long batchId, Long itemId) {
        if (orderId == null || driverUserId == null || itemId == null) {
            return new PayoutResult(false, null, "missing payout identifiers");
        }
        if (netAmount == null || netAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new PayoutResult(false, null, "invalid payout amount");
        }
        if (isBlank(payoutSecretKey)) {
            return new PayoutResult(false, null, "payment.toss.payout.secret-key is required");
        }

        Driver driver = driverRepository.findByUser_UserId(driverUserId).orElse(null);
        if (driver == null) {
            return new PayoutResult(false, null, "driver profile not found");
        }

        String bankName = normalize(driver.getBankName());
        String accountNumber = normalize(driver.getAccountNum());
        if (isBlank(bankName) || isBlank(accountNumber)) {
            return new PayoutResult(false, null, "driver bank/account is required");
        }

        String payoutId = createPayoutId(orderId, batchId, itemId);
        String auth = encodeBasicAuth(payoutSecretKey);
        PayoutRequest body = new PayoutRequest(
                payoutId,
                String.valueOf(orderId),
                String.valueOf(driverUserId),
                netAmount,
                bankName,
                accountNumber,
                "KRW",
                "BAROTRUCK_DRIVER_PAYOUT"
        );

        String responseBody;
        try {
            responseBody = webClientBuilder.baseUrl(tossBaseUrl).build()
                    .post()
                    .uri(payoutPath)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(
                            status -> status.value() >= 400,
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new IllegalStateException(errorBody)))
                    )
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            String reason = normalize(e.getMessage());
            log.warn("toss payout request failed. orderId={}, driverUserId={}, reason={}", orderId, driverUserId, reason);
            return new PayoutResult(false, null, defaultIfBlank(reason, "toss payout http error"));
        }

        return parsePayoutResponse(payoutId, responseBody);
    }

    private PayoutResult parsePayoutResponse(String fallbackPayoutRef, String rawBody) {
        if (isBlank(rawBody)) {
            return new PayoutResult(true, fallbackPayoutRef, null);
        }

        try {
            JsonNode node = objectMapper.readTree(rawBody);
            String status = firstNonBlank(
                    readText(node, "status"),
                    readText(node, "result"),
                    readText(node, "payoutStatus")
            );
            if (isFailureStatus(status)) {
                String failReason = firstNonBlank(
                        readText(node, "message"),
                        readText(node, "reason"),
                        readText(node, "code"),
                        "toss payout failed"
                );
                return new PayoutResult(false, null, failReason);
            }

            String payoutRef = firstNonBlank(
                    readText(node, "payoutId"),
                    readText(node, "payoutKey"),
                    readText(node, "transferId"),
                    readText(node, "transactionId"),
                    readText(node, "id"),
                    fallbackPayoutRef
            );
            return new PayoutResult(true, payoutRef, null);
        } catch (Exception e) {
            String failReason = defaultIfBlank(e.getMessage(), "toss payout response parse error");
            return new PayoutResult(false, null, failReason);
        }
    }

    private boolean isFailureStatus(String status) {
        if (isBlank(status)) {
            return false;
        }
        String upper = status.trim().toUpperCase();
        return upper.contains("FAIL")
                || upper.contains("ERROR")
                || upper.contains("REJECT")
                || upper.contains("DENY")
                || upper.contains("CANCEL");
    }

    private String createPayoutId(Long orderId, Long batchId, Long itemId) {
        if (batchId == null) {
            return "PAYOUT-" + orderId + "-" + itemId + "-" + UUID.randomUUID();
        }
        return "PAYOUT-" + batchId + "-" + itemId;
    }

    private String encodeBasicAuth(String secretKey) {
        return Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }

    private String readText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null ? null : text.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private record PayoutRequest(
            String payoutId,
            String orderId,
            String driverUserId,
            BigDecimal amount,
            String bankName,
            String accountNumber,
            String currency,
            String description
    ) {
    }
}

