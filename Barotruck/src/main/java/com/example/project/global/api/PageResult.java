package com.example.project.global.api;

import java.util.List;

public record PageResult<T>(
        List<T> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        int numberOfElements,
        boolean first,
        boolean last,
        boolean empty
) {
    public static <T> PageResult<T> of(List<T> content, int number, int size, long totalElements) {
        int safeSize = Math.max(size, 1);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);
        int numberOfElements = content == null ? 0 : content.size();
        boolean empty = numberOfElements == 0;
        boolean first = number <= 0;
        boolean last = empty || totalPages == 0 || number >= totalPages - 1;

        return new PageResult<>(
                content,
                number,
                safeSize,
                totalElements,
                totalPages,
                numberOfElements,
                first,
                last,
                empty
        );
    }
}
