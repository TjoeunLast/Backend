package com.example.project.global.toss.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class TossPaymentHttpClient implements TossPaymentClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${payment.toss.base-url:https://api.tosspayments.com}")
    private String tossBaseUrl;

    @Value("${payment.toss.secret-key:}")
    private String tossSecretKey;

    @Value("${payment.toss.billing.secret-key:${payment.toss.secret-key:}}")
    private String tossBillingSecretKey;

    @Override
    public ConfirmResult confirm(String paymentKey, String pgOrderId, BigDecimal amount) {
        if (isBlank(tossSecretKey)) {
            return failureConfirmResult(
                    "TOSS_SECRET_KEY_MISSING",
                    "payment.toss.secret-key is required for real toss confirm",
                    null
            );
        }
        if (isBlank(paymentKey)) {
            return failureConfirmResult("INVALID_PAYMENT_KEY", "paymentKey is required", null);
        }
        if (isBlank(pgOrderId)) {
            return failureConfirmResult("INVALID_ORDER_ID", "orderId is required", null);
        }
        if (!isPositive(amount)) {
            return failureConfirmResult("INVALID_AMOUNT", "amount must be positive", null);
        }

        try {
            String responseBody = postJson(
                    tossSecretKey,
                    "/v1/payments/confirm",
                    new ConfirmBody(paymentKey, pgOrderId, amount),
                    null
            );
            JsonNode node = readJsonNode(
                    responseBody,
                    "TOSS_CONFIRM_PARSE_ERROR",
                    "failed to parse toss confirm response"
            );
            return new ConfirmResult(
                    true,
                    firstNonBlank(resolveTransactionId(node), paymentKey),
                    null,
                    null,
                    responseBody,
                    readText(node, "method"),
                    readEasyPayProvider(node)
            );
        } catch (Exception e) {
            return failureConfirmResult(resolveFailCode(e), resolveFailMessage(e), resolveRawPayload(e));
        }
    }

    @Override
    public LookupResult lookupByPaymentKey(String paymentKey) {
        if (isBlank(tossSecretKey)) {
            return missingSecretLookupResult();
        }
        if (isBlank(paymentKey)) {
            return failureLookupResult("INVALID_PAYMENT_KEY", "paymentKey is required", null, null, null);
        }
        try {
            String responseBody = getJson(tossSecretKey, "/v1/payments/" + paymentKey);
            return parseLookupResult(responseBody);
        } catch (Exception e) {
            return failureLookupResult(resolveFailCode(e), resolveFailMessage(e), resolveRawPayload(e), paymentKey, null);
        }
    }

    @Override
    public LookupResult lookupByOrderId(String orderId) {
        if (isBlank(tossSecretKey)) {
            return missingSecretLookupResult();
        }
        if (isBlank(orderId)) {
            return failureLookupResult("INVALID_ORDER_ID", "orderId is required", null, null, null);
        }
        try {
            String responseBody = getJson(tossSecretKey, "/v1/payments/orders/" + orderId);
            return parseLookupResult(responseBody);
        } catch (Exception e) {
            return failureLookupResult(resolveFailCode(e), resolveFailMessage(e), resolveRawPayload(e), null, orderId);
        }
    }

    @Override
    public CancelResult cancel(String paymentKey, String cancelReason, BigDecimal cancelAmount) {
        if (isBlank(tossSecretKey)) {
            return failureCancelResult(
                    "TOSS_SECRET_KEY_MISSING",
                    "payment.toss.secret-key is required for real toss cancel",
                    null,
                    paymentKey,
                    cancelAmount
            );
        }
        if (isBlank(paymentKey)) {
            return failureCancelResult("INVALID_PAYMENT_KEY", "paymentKey is required", null, null, cancelAmount);
        }
        if (cancelAmount != null && !isPositive(cancelAmount)) {
            return failureCancelResult("INVALID_CANCEL_AMOUNT", "cancelAmount must be positive", null, paymentKey, cancelAmount);
        }

        try {
            String responseBody = postJson(
                    tossSecretKey,
                    "/v1/payments/" + paymentKey + "/cancel",
                    new CancelBody(defaultIfBlank(cancelReason, "admin cancel"), cancelAmount),
                    UUID.randomUUID().toString()
            );
            JsonNode node = readJsonNode(
                    responseBody,
                    "TOSS_CANCEL_PARSE_ERROR",
                    "failed to parse toss cancel response"
            );
            List<CancelHistory> cancels = readCancels(node);
            CancelHistory latest = resolveLatestCancel(cancels);
            BigDecimal resolvedCancelAmount = firstNonNull(
                    latest == null ? null : latest.cancelAmount(),
                    readDecimal(node, "cancelAmount"),
                    readDecimal(node, "canceledAmount"),
                    cancelAmount
            );
            LocalDateTime resolvedCanceledAt = firstNonNull(
                    latest == null ? null : latest.canceledAt(),
                    parseDateTime(readText(node, "lastTransactionAt")),
                    parseDateTime(readText(node, "approvedAt"))
            );
            String resolvedStatus = firstNonBlank(
                    readText(node, "status"),
                    latest == null ? null : latest.status(),
                    resolvedCancelAmount == null ? null : "CANCELED"
            );
            return new CancelResult(
                    true,
                    null,
                    null,
                    responseBody,
                    firstNonBlank(readText(node, "paymentKey"), paymentKey),
                    firstNonBlank(latest == null ? null : latest.transactionKey(), resolveTransactionId(node), paymentKey),
                    resolvedCancelAmount,
                    resolvedCanceledAt,
                    resolvedStatus
            );
        } catch (Exception e) {
            return failureCancelResult(resolveFailCode(e), resolveFailMessage(e), resolveRawPayload(e), paymentKey, cancelAmount);
        }
    }

    @Override
    public BillingIssueResult issueBillingKey(String authKey, String customerKey) {
        if (isBlank(tossBillingSecretKey)) {
            return failureBillingIssueResult(
                    "TOSS_BILLING_SECRET_KEY_MISSING",
                    "payment.toss.billing.secret-key is required",
                    null,
                    customerKey
            );
        }
        if (isBlank(authKey)) {
            return failureBillingIssueResult("INVALID_AUTH_KEY", "authKey is required", null, customerKey);
        }
        if (isBlank(customerKey)) {
            return failureBillingIssueResult("INVALID_CUSTOMER_KEY", "customerKey is required", null, null);
        }

        try {
            String responseBody = postJson(
                    tossBillingSecretKey,
                    "/v1/billing/authorizations/issue",
                    new BillingIssueBody(authKey, customerKey),
                    UUID.randomUUID().toString()
            );
            JsonNode node = readJsonNode(
                    responseBody,
                    "TOSS_BILLING_ISSUE_PARSE_ERROR",
                    "failed to parse toss billing issue response"
            );
            String billingKey = readText(node, "billingKey");
            if (isBlank(billingKey)) {
                return failureBillingIssueResult(
                        "TOSS_BILLING_ISSUE_PARSE_ERROR",
                        "billingKey missing from toss billing issue response",
                        responseBody,
                        customerKey
                );
            }
            JsonNode cardNode = node.path("card");
            return new BillingIssueResult(
                    true,
                    null,
                    null,
                    responseBody,
                    firstNonBlank(readText(node, "customerKey"), customerKey),
                    billingKey,
                    readText(cardNode, "company"),
                    firstNonBlank(readText(cardNode, "number"), readText(cardNode, "cardNumber")),
                    readText(cardNode, "cardType"),
                    readText(cardNode, "ownerType")
            );
        } catch (Exception e) {
            return failureBillingIssueResult(resolveFailCode(e), resolveFailMessage(e), resolveRawPayload(e), customerKey);
        }
    }

    @Override
    public BillingChargeResult chargeBillingKey(
            String billingKey,
            String customerKey,
            String orderId,
            String orderName,
            BigDecimal amount
    ) {
        if (isBlank(tossBillingSecretKey)) {
            return failureBillingChargeResult(
                    "TOSS_BILLING_SECRET_KEY_MISSING",
                    "payment.toss.billing.secret-key is required",
                    null,
                    orderId,
                    amount
            );
        }
        if (isBlank(billingKey)) {
            return failureBillingChargeResult("INVALID_BILLING_KEY", "billingKey is required", null, orderId, amount);
        }
        if (isBlank(customerKey)) {
            return failureBillingChargeResult("INVALID_CUSTOMER_KEY", "customerKey is required", null, orderId, amount);
        }
        if (isBlank(orderId)) {
            return failureBillingChargeResult("INVALID_ORDER_ID", "orderId is required", null, null, amount);
        }
        if (isBlank(orderName)) {
            return failureBillingChargeResult("INVALID_ORDER_NAME", "orderName is required", null, orderId, amount);
        }
        if (!isPositive(amount)) {
            return failureBillingChargeResult("INVALID_AMOUNT", "amount must be positive", null, orderId, amount);
        }

        try {
            String responseBody = postJson(
                    tossBillingSecretKey,
                    "/v1/billing/" + billingKey,
                    new BillingChargeBody(customerKey, orderId, orderName, amount),
                    UUID.randomUUID().toString()
            );
            JsonNode node = readJsonNode(
                    responseBody,
                    "TOSS_BILLING_CHARGE_PARSE_ERROR",
                    "failed to parse toss billing charge response"
            );
            String paymentKey = readText(node, "paymentKey");
            String resolvedStatus = readText(node, "status");
            if (isBlank(paymentKey) || isBlank(resolvedStatus)) {
                return failureBillingChargeResult(
                        "TOSS_BILLING_CHARGE_PARSE_ERROR",
                        "paymentKey or status missing from toss billing charge response",
                        responseBody,
                        orderId,
                        amount
                );
            }
            return new BillingChargeResult(
                    true,
                    null,
                    null,
                    responseBody,
                    paymentKey,
                    resolveTransactionId(node),
                    firstNonBlank(readText(node, "orderId"), orderId),
                    resolvedStatus,
                    firstNonNull(readDecimal(node, "totalAmount"), amount),
                    parseDateTime(firstNonBlank(
                            readText(node, "approvedAt"),
                            readText(node, "lastTransactionAt"),
                            readText(node, "requestedAt")
                    ))
            );
        } catch (Exception e) {
            return failureBillingChargeResult(resolveFailCode(e), resolveFailMessage(e), resolveRawPayload(e), orderId, amount);
        }
    }

    @Override
    public BillingDeleteResult deleteBillingKey(String billingKey, String customerKey) {
        if (isBlank(tossBillingSecretKey)) {
            return failureBillingDeleteResult(
                    "TOSS_BILLING_SECRET_KEY_MISSING",
                    "payment.toss.billing.secret-key is required",
                    null
            );
        }
        if (isBlank(billingKey)) {
            return failureBillingDeleteResult("INVALID_BILLING_KEY", "billingKey is required", null);
        }
        if (isBlank(customerKey)) {
            return failureBillingDeleteResult("INVALID_CUSTOMER_KEY", "customerKey is required", null);
        }

        try {
            String path = "/v1/billing/" + billingKey;
            String responseBody = deleteJson(tossBillingSecretKey, path, customerKey);
            return new BillingDeleteResult(true, null, null, responseBody);
        } catch (Exception e) {
            return failureBillingDeleteResult(resolveFailCode(e), resolveFailMessage(e), resolveRawPayload(e));
        }
    }

    private LookupResult missingSecretLookupResult() {
        return failureLookupResult(
                "TOSS_SECRET_KEY_MISSING",
                "payment.toss.secret-key is required for toss lookup",
                null,
                null,
                null
        );
    }

    private LookupResult parseLookupResult(String responseBody) throws Exception {
        JsonNode node = readJsonNode(
                responseBody,
                "TOSS_LOOKUP_PARSE_ERROR",
                "failed to parse toss lookup response"
        );
        List<CancelHistory> cancels = readCancels(node);
        return new LookupResult(
                true,
                null,
                null,
                responseBody,
                readText(node, "paymentKey"),
                readText(node, "orderId"),
                firstNonBlank(readText(node, "status"), cancels.isEmpty() ? null : "CANCELED"),
                readText(node, "method"),
                readEasyPayProvider(node),
                readDecimal(node, "totalAmount"),
                readDecimal(node, "suppliedAmount"),
                readDecimal(node, "vat"),
                parseDateTime(readText(node, "approvedAt")),
                firstNonNull(
                        parseDateTime(readText(node, "lastTransactionAt")),
                        parseDateTime(readText(node, "approvedAt"))
                ),
                cancels
        );
    }

    private List<CancelHistory> readCancels(JsonNode node) {
        JsonNode cancelsNode = node == null ? null : node.get("cancels");
        if (cancelsNode == null || !cancelsNode.isArray()) {
            return List.of();
        }

        List<CancelHistory> cancels = new ArrayList<>();
        for (JsonNode cancelNode : cancelsNode) {
            cancels.add(new CancelHistory(
                    firstNonNull(readDecimal(cancelNode, "cancelAmount"), readDecimal(cancelNode, "canceledAmount")),
                    firstNonBlank(readText(cancelNode, "cancelReason"), readText(cancelNode, "reason")),
                    parseDateTime(firstNonBlank(readText(cancelNode, "canceledAt"), readText(cancelNode, "approvedAt"))),
                    firstNonBlank(readText(cancelNode, "transactionKey"), readText(cancelNode, "cancelTransactionKey")),
                    firstNonBlank(readText(cancelNode, "cancelStatus"), readText(cancelNode, "status"))
            ));
        }
        return cancels;
    }

    private CancelHistory resolveLatestCancel(List<CancelHistory> cancels) {
        if (cancels == null || cancels.isEmpty()) {
            return null;
        }
        CancelHistory latest = null;
        for (CancelHistory cancel : cancels) {
            if (cancel == null) {
                continue;
            }
            if (latest == null) {
                latest = cancel;
                continue;
            }
            if (latest.canceledAt() == null && cancel.canceledAt() != null) {
                latest = cancel;
                continue;
            }
            if (latest.canceledAt() != null && cancel.canceledAt() != null && cancel.canceledAt().isAfter(latest.canceledAt())) {
                latest = cancel;
                continue;
            }
            if (latest.canceledAt() == null && cancel.canceledAt() == null) {
                latest = cancel;
            }
        }
        return latest;
    }

    private String postJson(String secretKey, String path, Object body, String idempotencyKey) {
        return webClientBuilder.baseUrl(tossBaseUrl).build()
                .post()
                .uri(path)
                .headers(headers -> applyHeaders(headers, secretKey, idempotencyKey))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        status -> status.value() >= 400,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(errorBody -> Mono.error(toTossClientException(clientResponse.statusCode().value(), errorBody)))
                )
                .bodyToMono(String.class)
                .defaultIfEmpty("")
                .block();
    }

    private String getJson(String secretKey, String path) {
        return webClientBuilder.baseUrl(tossBaseUrl).build()
                .get()
                .uri(path)
                .headers(headers -> applyHeaders(headers, secretKey, null))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        status -> status.value() >= 400,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(errorBody -> Mono.error(toTossClientException(clientResponse.statusCode().value(), errorBody)))
                )
                .bodyToMono(String.class)
                .defaultIfEmpty("")
                .block();
    }

    private String deleteJson(String secretKey, String path, String customerKey) {
        return webClientBuilder.baseUrl(tossBaseUrl).build()
                .delete()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path(path);
                    if (!isBlank(customerKey)) {
                        builder.queryParam("customerKey", customerKey);
                    }
                    return builder.build();
                })
                .headers(headers -> applyHeaders(headers, secretKey, UUID.randomUUID().toString()))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        status -> status.value() >= 400,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(errorBody -> Mono.error(toTossClientException(clientResponse.statusCode().value(), errorBody)))
                )
                .bodyToMono(String.class)
                .defaultIfEmpty("")
                .block();
    }

    private void applyHeaders(HttpHeaders headers, String secretKey, String idempotencyKey) {
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodeBasicAuth(secretKey));
        if (!isBlank(idempotencyKey)) {
            headers.set("Idempotency-Key", idempotencyKey);
        }
    }

    private String encodeBasicAuth(String secretKey) {
        return Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }

    private String resolveTransactionId(JsonNode node) {
        return firstNonBlank(
                readText(node, "transactionKey"),
                readText(node, "paymentKey"),
                readText(node, "lastTransactionKey"),
                readText(node, "mId")
        );
    }

    private String readEasyPayProvider(JsonNode node) {
        JsonNode easyPayNode = node == null ? null : node.get("easyPay");
        return firstNonBlank(
                readText(easyPayNode, "provider"),
                readText(easyPayNode, "providerCode")
        );
    }

    private BigDecimal readDecimal(JsonNode node, String fieldName) {
        String text = readText(node, fieldName);
        if (isBlank(text)) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    private String readText(JsonNode node, String fieldName) {
        if (node == null || fieldName == null) {
            return null;
        }
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

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private <T> T firstNonNull(T first, T second, T third) {
        return first != null ? first : (second != null ? second : third);
    }

    private <T> T firstNonNull(T first, T second, T third, T fourth) {
        return first != null ? first : (second != null ? second : (third != null ? third : fourth));
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String resolveFailCode(Exception e) {
        if (e instanceof TossClientException clientException) {
            return clientException.code();
        }
        if (e instanceof JsonProcessingException) {
            return "TOSS_RESPONSE_PARSE_ERROR";
        }
        if (e instanceof WebClientRequestException) {
            return "TOSS_NETWORK_ERROR";
        }
        return "TOSS_CLIENT_ERROR";
    }

    private String resolveFailMessage(Exception e) {
        if (e instanceof TossClientException clientException) {
            return clientException.getMessage();
        }
        if (e instanceof JsonProcessingException jsonProcessingException) {
            return defaultIfBlank(jsonProcessingException.getOriginalMessage(), "failed to parse toss response");
        }
        return defaultIfBlank(e.getMessage(), "unknown toss client error");
    }

    private String resolveRawPayload(Exception e) {
        if (e instanceof TossClientException clientException) {
            return clientException.rawPayload();
        }
        if (e instanceof JsonProcessingException jsonProcessingException) {
            return jsonProcessingException.getOriginalMessage();
        }
        return e.getMessage();
    }

    private JsonNode readJsonNode(String responseBody, String failCode, String failMessage) {
        if (isBlank(responseBody)) {
            throw new TossClientException(failCode, failMessage, responseBody, null);
        }
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException e) {
            throw new TossClientException(
                    failCode,
                    defaultIfBlank(e.getOriginalMessage(), failMessage),
                    responseBody,
                    null
            );
        }
    }

    private TossClientException toTossClientException(int statusCode, String errorBody) {
        JsonNode node = null;
        if (!isBlank(errorBody)) {
            try {
                node = objectMapper.readTree(errorBody);
            } catch (Exception ignored) {
            }
        }
        String code = firstNonBlank(
                readText(node, "code"),
                readText(node, "errorCode"),
                "TOSS_HTTP_" + statusCode
        );
        String message = firstNonBlank(
                readText(node, "message"),
                readText(node, "error"),
                "toss http error (" + statusCode + ")"
        );
        return new TossClientException(code, message, errorBody, statusCode);
    }

    private ConfirmResult failureConfirmResult(String failCode, String failMessage, String rawPayload) {
        return new ConfirmResult(false, null, failCode, failMessage, rawPayload, null, null);
    }

    private LookupResult failureLookupResult(
            String failCode,
            String failMessage,
            String rawPayload,
            String paymentKey,
            String orderId
    ) {
        return new LookupResult(
                false,
                failCode,
                failMessage,
                rawPayload,
                paymentKey,
                orderId,
                "LOOKUP_FAILED",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    private CancelResult failureCancelResult(
            String failCode,
            String failMessage,
            String rawPayload,
            String paymentKey,
            BigDecimal cancelAmount
    ) {
        return new CancelResult(false, failCode, failMessage, rawPayload, paymentKey, null, cancelAmount, null, null);
    }

    private BillingIssueResult failureBillingIssueResult(
            String failCode,
            String failMessage,
            String rawPayload,
            String customerKey
    ) {
        return new BillingIssueResult(false, failCode, failMessage, rawPayload, customerKey, null, null, null, null, null);
    }

    private BillingChargeResult failureBillingChargeResult(
            String failCode,
            String failMessage,
            String rawPayload,
            String orderId,
            BigDecimal amount
    ) {
        return new BillingChargeResult(false, failCode, failMessage, rawPayload, null, null, orderId, null, amount, null);
    }

    private BillingDeleteResult failureBillingDeleteResult(String failCode, String failMessage, String rawPayload) {
        return new BillingDeleteResult(false, failCode, failMessage, rawPayload);
    }

    private record ConfirmBody(String paymentKey, String orderId, BigDecimal amount) {
    }

    private record CancelBody(String cancelReason, BigDecimal cancelAmount) {
    }

    private record BillingIssueBody(String authKey, String customerKey) {
    }

    private record BillingChargeBody(String customerKey, String orderId, String orderName, BigDecimal amount) {
    }

    private static final class TossClientException extends RuntimeException {
        private final String code;
        private final String rawPayload;
        private final Integer httpStatus;

        private TossClientException(String code, String message, String rawPayload, Integer httpStatus) {
            super(message);
            this.code = code;
            this.rawPayload = rawPayload;
            this.httpStatus = httpStatus;
        }

        private String code() {
            return code;
        }

        private String rawPayload() {
            return rawPayload;
        }

        private Integer httpStatus() {
            return httpStatus;
        }
    }
}
