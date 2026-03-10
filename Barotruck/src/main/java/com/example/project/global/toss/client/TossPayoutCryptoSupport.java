package com.example.project.global.toss.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

final class TossPayoutCryptoSupport {

    private TossPayoutCryptoSupport() {
    }

    static String encrypt(ObjectMapper objectMapper, Object payload, String securityKey, ZoneId zoneId) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            byte[] keyBytes = decodeHexKey(securityKey);
            JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
                    .customParam("iat", OffsetDateTime.now(zoneId).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                    .customParam("nonce", UUID.randomUUID().toString())
                    .build();
            JWEObject jweObject = new JWEObject(header, new Payload(json));
            jweObject.encrypt(new DirectEncrypter(keyBytes));
            return jweObject.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("failed to encrypt toss payout payload", e);
        }
    }

    static JsonNode parseMaybeEncryptedJson(ObjectMapper objectMapper, String rawBody, String securityKey) {
        String normalized = rawBody == null ? null : rawBody.trim();
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }

        try {
            if (looksLikeJwe(normalized)) {
                return objectMapper.readTree(decrypt(normalized, securityKey));
            }
            return objectMapper.readTree(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    static String decrypt(String compactJwe, String securityKey) {
        try {
            JWEObject jweObject = JWEObject.parse(compactJwe);
            jweObject.decrypt(new DirectDecrypter(decodeHexKey(securityKey)));
            return jweObject.getPayload().toString();
        } catch (Exception e) {
            throw new IllegalStateException("failed to decrypt toss payout payload", e);
        }
    }

    static boolean hasValidKey(String securityKey) {
        try {
            decodeHexKey(securityKey);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean looksLikeJwe(String value) {
        return value.chars().filter(ch -> ch == '.').count() == 4;
    }

    private static byte[] decodeHexKey(String securityKey) {
        String normalized = securityKey == null ? "" : securityKey.trim();
        if (normalized.length() != 64) {
            throw new IllegalArgumentException("payment.toss.payout.security-key must be 64 hex chars");
        }

        byte[] result = new byte[32];
        for (int i = 0; i < normalized.length(); i += 2) {
            int high = Character.digit(normalized.charAt(i), 16);
            int low = Character.digit(normalized.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("payment.toss.payout.security-key must be hex encoded");
            }
            result[i / 2] = (byte) ((high << 4) + low);
        }
        return result;
    }
}
