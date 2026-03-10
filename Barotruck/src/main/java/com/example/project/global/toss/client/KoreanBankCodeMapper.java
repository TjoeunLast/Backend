package com.example.project.global.toss.client;

import java.util.Map;

final class KoreanBankCodeMapper {

    private static final Map<String, String> BANK_CODES = Map.ofEntries(
            Map.entry("002", "002"),
            Map.entry("KDB", "002"),
            Map.entry("산업은행", "002"),
            Map.entry("003", "003"),
            Map.entry("IBK", "003"),
            Map.entry("기업은행", "003"),
            Map.entry("004", "004"),
            Map.entry("KB", "004"),
            Map.entry("KOOKMIN", "004"),
            Map.entry("KOOKMINBANK", "004"),
            Map.entry("국민", "004"),
            Map.entry("국민은행", "004"),
            Map.entry("007", "007"),
            Map.entry("수협", "007"),
            Map.entry("수협은행", "007"),
            Map.entry("011", "011"),
            Map.entry("NH", "011"),
            Map.entry("NONGHYUP", "011"),
            Map.entry("농협", "011"),
            Map.entry("농협은행", "011"),
            Map.entry("020", "020"),
            Map.entry("WOORI", "020"),
            Map.entry("우리", "020"),
            Map.entry("우리은행", "020"),
            Map.entry("023", "023"),
            Map.entry("SC", "023"),
            Map.entry("SCBANK", "023"),
            Map.entry("STANDARDCHARTERED", "023"),
            Map.entry("SC제일은행", "023"),
            Map.entry("SC제일", "023"),
            Map.entry("027", "027"),
            Map.entry("CITI", "027"),
            Map.entry("CITIBANK", "027"),
            Map.entry("씨티", "027"),
            Map.entry("씨티은행", "027"),
            Map.entry("031", "031"),
            Map.entry("대구", "031"),
            Map.entry("대구은행", "031"),
            Map.entry("032", "032"),
            Map.entry("부산", "032"),
            Map.entry("부산은행", "032"),
            Map.entry("034", "034"),
            Map.entry("광주", "034"),
            Map.entry("광주은행", "034"),
            Map.entry("035", "035"),
            Map.entry("제주", "035"),
            Map.entry("제주은행", "035"),
            Map.entry("037", "037"),
            Map.entry("전북", "037"),
            Map.entry("전북은행", "037"),
            Map.entry("039", "039"),
            Map.entry("경남", "039"),
            Map.entry("경남은행", "039"),
            Map.entry("045", "045"),
            Map.entry("새마을금고", "045"),
            Map.entry("MG", "045"),
            Map.entry("048", "048"),
            Map.entry("신협", "048"),
            Map.entry("071", "071"),
            Map.entry("우체국", "071"),
            Map.entry("우체국예금", "071"),
            Map.entry("081", "081"),
            Map.entry("HANA", "081"),
            Map.entry("KEB", "081"),
            Map.entry("하나", "081"),
            Map.entry("하나은행", "081"),
            Map.entry("088", "088"),
            Map.entry("SHINHAN", "088"),
            Map.entry("신한", "088"),
            Map.entry("신한은행", "088"),
            Map.entry("089", "089"),
            Map.entry("KBANK", "089"),
            Map.entry("케이뱅크", "089"),
            Map.entry("090", "090"),
            Map.entry("KAKAOBANK", "090"),
            Map.entry("카카오뱅크", "090"),
            Map.entry("092", "092"),
            Map.entry("TOSSBANK", "092"),
            Map.entry("토스뱅크", "092")
    );

    private KoreanBankCodeMapper() {
    }

    static String toBankCode(String bankNameOrCode) {
        String normalized = normalize(bankNameOrCode);
        if (normalized == null) {
            return null;
        }
        return BANK_CODES.get(normalized);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        normalized = normalized
                .replace("주식회사", "")
                .replace("(주)", "")
                .replace("-", "")
                .replace(" ", "")
                .toUpperCase();
        return normalized;
    }
}
