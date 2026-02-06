package com.example.project.global.neighborhood;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class KoreanAddressParser {
    private KoreanAddressParser() {}

    private static final Pattern PAREN = Pattern.compile("\\(.*?\\)");

    // “시/도” 레벨의 ‘시’들은 최소셋으로만 하드코딩 (수원시 같은 건 시/도 아님)
    private static final Set<String> METRO_SI_SET = Set.of(
            "서울특별시","부산광역시","대구광역시","인천광역시","광주광역시",
            "대전광역시","울산광역시","세종특별자치시"
    );

    public static ParsedAddress parse(String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank()) {
            return new ParsedAddress(null, null);
        }

        List<String> tokens = tokenize(fullAddress);
        if (tokens.isEmpty()) return new ParsedAddress(null, null);

        String cityName = resolveCityName(tokens);
        if (cityName == null) return new ParsedAddress(null, null);

        // ✅ 세종 정책: DB가 displayName="세종시"로 고정이면 파서도 고정
        if ("세종특별자치시".equals(cityName)) {
            return new ParsedAddress(cityName, "세종시");
        }

        // 일반 케이스: displayName 후보를 만들어 DB 정책에 맞게 선택(여기서는 “기본 2단계 행정구역”)
        String displayName = resolveDisplayName(tokens, cityName);

        return new ParsedAddress(cityName, displayName);
    }

    private static List<String> tokenize(String fullAddress) {
        String normalized = PAREN.matcher(fullAddress).replaceAll(" ")
                .replace(",", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isEmpty()) return List.of();
        return Arrays.asList(normalized.split(" "));
    }

    private static String resolveCityName(List<String> tokens) {
        String t0 = tokens.get(0);

        // 1) 광역시/특별시/세종 같은 “시/도 레벨 시”
        if (METRO_SI_SET.contains(t0)) return t0;

        // 2) “도”는 규칙으로 시/도 확정 가능 (경기도, 충청남도 등)
        if (t0.endsWith("도")) return t0;

        // 3) 그 외(예: "수원시 ..."로 시작)는 여기선 시/도 추출 실패로 처리
        return null;
    }

    private static String resolveDisplayName(List<String> tokens, String cityName) {
        if (tokens.size() < 2) return null;

        // 세종: DB 정책 고정
        if ("세종특별자치시".equals(cityName)) {
            return "세종시";
        }

        String t1 = tokens.get(1);

        // 도: displayName은 "시" 또는 "군"만 허용 (구는 절대 안씀)
        if (cityName.endsWith("도")) {
            if (t1.endsWith("시") || t1.endsWith("군")) return t1;
            return null;
        }

        // 특별시/광역시: displayName은 "구"가 보통, 예외로 "군/시"도 허용
        if (t1.endsWith("구") || t1.endsWith("군") || t1.endsWith("시")) return t1;

        return null;
    }
}
