package com.example.project.global.hashid;

import org.hashids.Hashids;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class HashidToLongConverter implements Converter<String, Long> {

    private final Hashids hashids;

    public HashidToLongConverter(Hashids hashids) {
        this.hashids = hashids;
    }

    @Override
    public Long convert(String source) {
        try {
            if (source == null || source.isBlank()) {
                return null;
            }

            if (source.matches("^[0-9]+$")) {
                return Long.parseLong(source);
            }

            long[] decoded = hashids.decode(source);
            if (decoded.length > 0) {
                return decoded[0];
            }
        } catch (Exception e) {
            return null;
        }
        return null; // 여기서 null을 주면 컨트롤러가 Bad Request를 냅니다.
    }
}
