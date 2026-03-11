package com.example.project.global.toss.client;

import com.example.project.domain.payment.service.client.DriverPayoutGatewayClient;
import com.example.project.member.domain.Driver;
import com.example.project.member.domain.Users;
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
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.payout.mock-enabled", havingValue = "false")
public class TossDriverPayoutGatewayClient implements DriverPayoutGatewayClient {

    private static final String HEADER_API_SECURITY_MODE = "TossPayments-api-security-mode";
    private static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";
    private static final String SECURITY_MODE_ENCRYPTION = "ENCRYPTION";

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final DriverRepository driverRepository;

    @Value("${payment.toss.base-url:https://api.tosspayments.com}")
    private String tossBaseUrl;

    @Value("${payment.toss.payout.seller-path:/v2/sellers}")
    private String sellerPath;

    @Value("${payment.toss.payout.path:/v2/payouts}")
    private String payoutPath;

    @Value("${payment.toss.payout.secret-key:${payment.toss.secret-key:}}")
    private String payoutSecretKey;

    @Value("${payment.toss.payout.security-key:}")
    private String payoutSecurityKey;

    @Value("${payment.toss.payout.schedule-type:AUTO}")
    private String payoutScheduleType;

    @Value("${payment.toss.payout.timezone:Asia/Seoul}")
    private String payoutTimezone;

    @Value("${payment.toss.payout.scheduled-hour:09}")
    private int payoutScheduledHour;

    @Value("${payment.toss.payout.scheduled-minute:00}")
    private int payoutScheduledMinute;

    @Value("${payment.toss.payout.test-corporate.company-name:}")
    private String testCorporateCompanyName;

    @Value("${payment.toss.payout.test-corporate.business-registration-number:}")
    private String testCorporateBusinessRegistrationNumber;

    @Override
    public PayoutResult payout(Long orderId, Long driverUserId, BigDecimal netAmount, Long batchId, Long itemId) {
        if (orderId == null || driverUserId == null || itemId == null) {
            return new PayoutResult(false, false, null, null, "missing payout identifiers");
        }
        if (netAmount == null || netAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new PayoutResult(false, false, null, null, "invalid payout amount");
        }
        if (isBlank(payoutSecretKey)) {
            return new PayoutResult(false, false, null, null, "payment.toss.payout.secret-key is required");
        }
        if (!TossPayoutCryptoSupport.hasValidKey(payoutSecurityKey)) {
            return new PayoutResult(false, false, null, null, "payment.toss.payout.security-key is required");
        }

        Driver driver = driverRepository.findByUser_UserId(driverUserId).orElse(null);
        if (driver == null) {
            return new PayoutResult(false, false, null, null, "driver profile not found");
        }

        Users driverUser = driver.getUser();
        if (driverUser == null) {
            return new PayoutResult(false, false, null, null, "driver user not found");
        }
        if (shouldBypassTestPayout()) {
            ensureTestSellerSnapshot(driver, driverUser);
            return new PayoutResult(
                    true,
                    true,
                    createTestPayoutRef(orderId, itemId),
                    "COMPLETED",
                    null
            );
        }

        BigDecimal payoutAmount = normalizePayoutAmount(netAmount);
        if (payoutAmount == null) {
            return new PayoutResult(false, false, null, null, "fractional KRW payout amount is not supported");
        }

        SellerInfo sellerInfo;
        try {
            sellerInfo = ensureSeller(driver, driverUser);
        } catch (Exception e) {
            String reason = defaultIfBlank(extractFailureReason(e), "failed to prepare seller");
            log.warn("toss seller ensure failed. driverUserId={}, reason={}", driverUserId, reason);
            return new PayoutResult(false, false, null, null, reason);
        }

        if (!isSellerPayoutReady(sellerInfo.status())) {
            String reason = "seller payout not ready. status=" + defaultIfBlank(sellerInfo.status(), "UNKNOWN");
            return new PayoutResult(false, false, null, sellerInfo.status(), reason);
        }

        String refPayoutId = createRefPayoutId(orderId, batchId, itemId);
        ResolvedSchedule schedule = resolveSchedule(LocalDateTime.now(resolveZoneId()));
        PayoutRequest body = new PayoutRequest(
                refPayoutId,
                sellerInfo.sellerId(),
                schedule.scheduleType(),
                schedule.scheduledAt(),
                new PayoutAmount(payoutAmount, "KRW"),
                createTransactionDescription(orderId)
        );

        try {
            JsonNode node = encryptedPostJson(payoutPath, body, refPayoutId);
            if (node == null) {
                return new PayoutResult(false, false, null, null, "empty toss payout response");
            }

            String payoutStatus = firstNonBlank(
                    readText(node, "status"),
                    readText(node, "payoutStatus")
            );
            if (isFailureStatus(payoutStatus)) {
                return new PayoutResult(
                        false,
                        false,
                        null,
                        payoutStatus,
                        firstNonBlank(readText(node, "message"), readText(node, "reason"), readText(node, "code"), "toss payout failed")
                );
            }

            String payoutRef = firstNonBlank(
                    readText(node, "payoutId"),
                    readText(node, "id"),
                    readText(node, "payoutKey"),
                    refPayoutId
            );
            boolean completed = isCompletedStatus(payoutStatus);
            return new PayoutResult(true, completed, payoutRef, payoutStatus, null);
        } catch (Exception e) {
            String reason = defaultIfBlank(extractFailureReason(e), "toss payout http error");
            log.warn("toss payout request failed. orderId={}, driverUserId={}, reason={}", orderId, driverUserId, reason);
            return new PayoutResult(false, false, null, null, reason);
        }
    }

    @Override
    public PayoutStatusResult getPayoutStatus(String payoutRef) {
        if (isBlank(payoutRef)) {
            return new PayoutStatusResult(false, false, false, null, "missing payoutRef");
        }
        if (shouldBypassTestPayout()) {
            return new PayoutStatusResult(true, true, false, "COMPLETED", null);
        }
        if (isBlank(payoutSecretKey)) {
            return new PayoutStatusResult(false, false, false, null, "payment.toss.payout.secret-key is required");
        }

        try {
            JsonNode node = getJson(buildPath(payoutPath, payoutRef));
            if (node == null) {
                return new PayoutStatusResult(false, false, false, null, "empty toss payout status response");
            }

            String payoutStatus = firstNonBlank(
                    readText(node, "status"),
                    readText(node, "payoutStatus")
            );
            if (isFailureStatus(payoutStatus)) {
                return new PayoutStatusResult(
                        true,
                        false,
                        true,
                        payoutStatus,
                        firstNonBlank(readText(node, "message"), readText(node, "reason"), readText(node, "code"), "toss payout failed")
                );
            }

            return new PayoutStatusResult(true, isCompletedStatus(payoutStatus), false, payoutStatus, null);
        } catch (Exception e) {
            return new PayoutStatusResult(false, false, false, null, defaultIfBlank(extractFailureReason(e), "failed to fetch payout status"));
        }
    }

    private SellerInfo ensureSeller(Driver driver, Users driverUser) {
        String existingSellerId = normalize(driver.getTossPayoutSellerId());
        if (!isBlank(existingSellerId)) {
            SellerInfo existingSeller = getSeller(existingSellerId);
            if (existingSeller != null) {
                updateDriverSellerSnapshot(driver, existingSeller);
                return existingSeller;
            }
            throw new IllegalStateException("failed to load existing toss seller: " + existingSellerId);
        }

        String bankCode = KoreanBankCodeMapper.toBankCode(driver.getBankName());
        if (isBlank(bankCode)) {
            throw new IllegalStateException("unsupported driver bank name: " + defaultIfBlank(driver.getBankName(), "EMPTY"));
        }

        String holderName = normalize(driverUser.getName());
        String email = normalize(driverUser.getEmail());
        String phone = digitsOnly(driverUser.getPhone());
        String accountNumber = digitsOnly(driver.getAccountNum());
        if (isBlank(holderName) || isBlank(email) || isBlank(phone) || isBlank(accountNumber)) {
            throw new IllegalStateException("driver seller registration requires name, email, phone, bank and account");
        }
        String refSellerId = defaultIfBlank(driver.getTossPayoutSellerRef(), createRefSellerId(driverUser.getUserId()));
        SellerRegisterRequest body;
        if (isTestPayoutSecretKey()) {
            String companyName = normalize(testCorporateCompanyName);
            String businessRegistrationNumber = digitsOnly(testCorporateBusinessRegistrationNumber);
            if (isBlank(companyName) || isBlank(businessRegistrationNumber) || businessRegistrationNumber.length() != 10) {
                throw new IllegalStateException(
                        "테스트 지급용 CORPORATE seller 정보가 없습니다. TOSS_PAYOUT_TEST_COMPANY_NAME, TOSS_PAYOUT_TEST_BUSINESS_REGISTRATION_NUMBER(10자리)를 설정하세요."
                );
            }
            body = new SellerRegisterRequest(
                    refSellerId,
                    "CORPORATE",
                    new SellerCompany(companyName, holderName, businessRegistrationNumber, email, phone),
                    null,
                    new SellerAccount(bankCode, accountNumber, holderName)
            );
        } else {
            body = new SellerRegisterRequest(
                    refSellerId,
                    "INDIVIDUAL",
                    null,
                    new SellerIndividual(holderName, email, phone),
                    new SellerAccount(bankCode, accountNumber, holderName)
            );
        }

        JsonNode node = encryptedPostJson(sellerPath, body, refSellerId);
        if (node == null) {
            throw new IllegalStateException("empty seller registration response");
        }

        SellerInfo registeredSeller = parseSellerInfo(node, refSellerId);
        if (registeredSeller == null) {
            throw new IllegalStateException("failed to parse seller registration response");
        }
        updateDriverSellerSnapshot(driver, registeredSeller);
        return registeredSeller;
    }

    private SellerInfo getSeller(String sellerId) {
        try {
            JsonNode node = getJson(buildPath(sellerPath, sellerId));
            return parseSellerInfo(node, null);
        } catch (Exception e) {
            log.warn("failed to fetch toss seller. sellerId={}, reason={}", sellerId, extractFailureReason(e));
            return null;
        }
    }

    private void updateDriverSellerSnapshot(Driver driver, SellerInfo sellerInfo) {
        driver.setTossPayoutSellerId(sellerInfo.sellerId());
        driver.setTossPayoutSellerRef(defaultIfBlank(sellerInfo.refSellerId(), driver.getTossPayoutSellerRef()));
        driver.setTossPayoutSellerStatus(sellerInfo.status());
        driverRepository.save(driver);
    }

    private void ensureTestSellerSnapshot(Driver driver, Users driverUser) {
        if (driver == null || driverUser == null) {
            return;
        }
        if (isBlank(driver.getTossPayoutSellerId())) {
            driver.setTossPayoutSellerId("TEST-SELLER-" + driverUser.getUserId());
        }
        if (isBlank(driver.getTossPayoutSellerRef())) {
            driver.setTossPayoutSellerRef(createRefSellerId(driverUser.getUserId()));
        }
        if (isBlank(driver.getTossPayoutSellerStatus())) {
            driver.setTossPayoutSellerStatus("APPROVED");
        }
        driverRepository.save(driver);
    }

    private JsonNode getJson(String path) {
        String responseBody = webClientBuilder.baseUrl(tossBaseUrl).build()
                .get()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodeBasicAuth(payoutSecretKey))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        status -> status.value() >= 400,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new IllegalStateException(errorBody)))
                )
                .bodyToMono(String.class)
                .block();

        JsonNode node = TossPayoutCryptoSupport.parseMaybeEncryptedJson(objectMapper, responseBody, payoutSecurityKey);
        if (node == null) {
            throw new IllegalStateException("invalid toss response");
        }
        return node;
    }

    private JsonNode encryptedPostJson(String path, Object body, String idempotencyKey) {
        String encryptedBody = TossPayoutCryptoSupport.encrypt(objectMapper, body, payoutSecurityKey, resolveZoneId());
        String responseBody = webClientBuilder.baseUrl(tossBaseUrl).build()
                .post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodeBasicAuth(payoutSecretKey))
                .header(HEADER_API_SECURITY_MODE, SECURITY_MODE_ENCRYPTION)
                .header(HEADER_IDEMPOTENCY_KEY, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(encryptedBody)
                .retrieve()
                .onStatus(
                        status -> status.value() >= 400,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new IllegalStateException(errorBody)))
                )
                .bodyToMono(String.class)
                .block();

        JsonNode node = TossPayoutCryptoSupport.parseMaybeEncryptedJson(objectMapper, responseBody, payoutSecurityKey);
        if (node == null) {
            throw new IllegalStateException("invalid toss response");
        }
        return node;
    }

    private SellerInfo parseSellerInfo(JsonNode node, String fallbackRefSellerId) {
        if (node == null || node.isNull()) {
            return null;
        }
        String sellerId = firstNonBlank(readText(node, "sellerId"), readText(node, "id"));
        String sellerStatus = firstNonBlank(readText(node, "status"), readText(node, "sellerStatus"));
        String refSellerId = firstNonBlank(readText(node, "refSellerId"), fallbackRefSellerId);
        if (isBlank(sellerId)) {
            return null;
        }
        return new SellerInfo(sellerId, refSellerId, sellerStatus);
    }

    private boolean isSellerPayoutReady(String status) {
        if (isBlank(status)) {
            return false;
        }
        String upper = status.toUpperCase(Locale.ROOT);
        return upper.contains("APPROVED");
    }

    private String createRefSellerId(Long userId) {
        String base = Long.toString(userId == null ? 0L : userId, 36).toUpperCase(Locale.ROOT);
        return trimToLength("SELL-" + base + "X", 20);
    }

    private String createRefPayoutId(Long orderId, Long batchId, Long itemId) {
        if (batchId != null && itemId != null) {
            return trimToLength(
                    "PO-" + Long.toString(batchId, 36).toUpperCase(Locale.ROOT)
                            + "-" + Long.toString(itemId, 36).toUpperCase(Locale.ROOT),
                    20
            );
        }
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        return trimToLength(
                "PO-" + Long.toString(orderId == null ? 0L : orderId, 36).toUpperCase(Locale.ROOT) + "-" + suffix,
                20
        );
    }

    private ResolvedSchedule resolveSchedule(LocalDateTime now) {
        String configured = defaultIfBlank(payoutScheduleType, "AUTO").toUpperCase(Locale.ROOT);
        if ("EXPRESS".equals(configured)) {
            return new ResolvedSchedule("EXPRESS", null);
        }
        if ("SCHEDULED".equals(configured)) {
            return new ResolvedSchedule("SCHEDULED", nextBusinessScheduledDate(now));
        }

        LocalTime currentTime = now.toLocalTime();
        boolean businessDay = isBusinessDay(now.toLocalDate());
        boolean withinExpressWindow =
                !currentTime.isBefore(LocalTime.of(8, 0)) && !currentTime.isAfter(LocalTime.of(15, 0));
        if (businessDay && withinExpressWindow) {
            return new ResolvedSchedule("EXPRESS", null);
        }
        return new ResolvedSchedule("SCHEDULED", nextBusinessScheduledDate(now));
    }

    private String nextBusinessScheduledDate(LocalDateTime now) {
        LocalDate date = now.toLocalDate();
        do {
            date = date.plusDays(1);
        } while (!isBusinessDay(date));
        return date.toString();
    }

    private boolean isBusinessDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    private int clampScheduleHour(int hour) {
        return Math.max(0, Math.min(hour, 23));
    }

    private int clampScheduleMinute(int minute) {
        return Math.max(0, Math.min(minute, 59));
    }

    private BigDecimal normalizePayoutAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        BigDecimal normalized = amount.stripTrailingZeros();
        if (normalized.scale() > 0) {
            return null;
        }
        return amount.setScale(0, RoundingMode.UNNECESSARY);
    }

    private ZoneId resolveZoneId() {
        try {
            return ZoneId.of(defaultIfBlank(payoutTimezone, "Asia/Seoul"));
        } catch (Exception e) {
            return ZoneId.of("Asia/Seoul");
        }
    }

    private String buildPath(String basePath, String pathValue) {
        return defaultIfBlank(basePath, "") + "/" + UriUtils.encodePathSegment(pathValue, StandardCharsets.UTF_8);
    }

    private boolean isCompletedStatus(String status) {
        if (isBlank(status)) {
            return false;
        }
        String upper = status.toUpperCase(Locale.ROOT);
        return upper.contains("COMPLETED") || upper.contains("DONE") || upper.contains("SUCCESS");
    }

    private boolean isFailureStatus(String status) {
        if (isBlank(status)) {
            return false;
        }
        String upper = status.toUpperCase(Locale.ROOT);
        return upper.contains("FAIL")
                || upper.contains("ERROR")
                || upper.contains("REJECT")
                || upper.contains("DENY")
                || upper.contains("CANCEL");
    }

    private String extractFailureReason(Exception exception) {
        String raw = normalize(exception == null ? null : exception.getMessage());
        String normalizedRawReason = toReadablePayoutFailureReason(raw);
        if (!isBlank(normalizedRawReason)) {
            return normalizedRawReason;
        }
        JsonNode node = TossPayoutCryptoSupport.parseMaybeEncryptedJson(objectMapper, raw, payoutSecurityKey);
        if (node == null) {
            if (looksLikeEncryptedPayload(raw)) {
                return "토스 지급 암호화 응답 복호화에 실패했습니다. TOSS_PAYOUT_SECRET_KEY / TOSS_PAYOUT_SECURITY_KEY 조합과 test/live 환경 일치를 확인하세요.";
            }
            return raw;
        }
        return toReadablePayoutFailureReason(firstNonBlank(
                readText(node, "message"),
                readText(node, "reason"),
                readText(node, "code"),
                raw
        ));
    }

    private String toReadablePayoutFailureReason(String value) {
        if (isBlank(value)) {
            return value;
        }

        String normalized = value.trim();
        String upper = normalized.toUpperCase(Locale.ROOT);

        if (upper.contains("GZIP")) {
            return "토스 지급 암호화 응답을 해석하지 못했습니다. TOSS_PAYOUT_SECRET_KEY / TOSS_PAYOUT_SECURITY_KEY가 같은 환경(test/live)의 지급용 키인지 확인하세요.";
        }
        if (upper.contains("FAILED TO DECRYPT TOSS PAYOUT")
                || upper.contains("INVALID_ENCRYPTION")
                || upper.contains("PAYMENT.TOSS.PAYOUT.SECURITY-KEY")) {
            return "토스 지급 응답 복호화에 실패했습니다. TOSS_PAYOUT_SECURITY_KEY와 지급용 secret/security key 조합을 확인하세요.";
        }
        if (upper.contains("PAYMENT.TOSS.PAYOUT.SECRET-KEY IS REQUIRED")) {
            return "토스 지급용 secret key가 없습니다. TOSS_PAYOUT_SECRET_KEY를 설정하세요.";
        }
        return normalized;
    }

    private String encodeBasicAuth(String secretKey) {
        return Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }

    private boolean isTestPayoutSecretKey() {
        return !isBlank(payoutSecretKey) && payoutSecretKey.trim().startsWith("test_sk_");
    }

    private boolean shouldBypassTestPayout() {
        return isTestPayoutSecretKey();
    }

    private String createTestPayoutRef(Long orderId, Long itemId) {
        if (orderId != null && itemId != null) {
            return "TEST-PAYOUT-" + orderId + "-" + itemId;
        }
        return "TEST-PAYOUT-" + UUID.randomUUID();
    }

    private String createTransactionDescription(Long orderId) {
        return trimToLength("정산" + (orderId == null ? "" : orderId), 7);
    }

    private boolean looksLikeEncryptedPayload(String value) {
        if (isBlank(value)) {
            return false;
        }
        return value.chars().filter(ch -> ch == '.').count() == 4;
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

    private String digitsOnly(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : digits;
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
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

    private record SellerRegisterRequest(
            String refSellerId,
            String businessType,
            SellerCompany company,
            SellerIndividual individual,
            SellerAccount account
    ) {
    }

    private record SellerCompany(
            String name,
            String representativeName,
            String businessRegistrationNumber,
            String email,
            String phone
    ) {
    }

    private record SellerIndividual(
            String name,
            String email,
            String phone
    ) {
    }

    private record SellerAccount(
            String bankCode,
            String accountNumber,
            String holderName
    ) {
    }

    private record PayoutRequest(
            String refPayoutId,
            String destination,
            String scheduleType,
            String payoutDate,
            PayoutAmount amount,
            String transactionDescription
    ) {
    }

    private record PayoutAmount(
            BigDecimal value,
            String currency
    ) {
    }

    private record SellerInfo(
            String sellerId,
            String refSellerId,
            String status
    ) {
    }

    private record ResolvedSchedule(
            String scheduleType,
            String scheduledAt
    ) {
    }
}
