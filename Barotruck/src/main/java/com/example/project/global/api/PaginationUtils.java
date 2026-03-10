package com.example.project.global.api;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.web.context.request.NativeWebRequest;

public final class PaginationUtils {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PaginationUtils() {
    }

    public static boolean isPagedRequest(NativeWebRequest webRequest) {
        if (webRequest == null) {
            return false;
        }
        return webRequest.getParameter("page") != null
                || webRequest.getParameter("size") != null
                || webRequest.getParameter("sort") != null;
    }

    public static <T> PageResult<T> paginate(List<T> items, Pageable pageable) {
        List<T> source = items == null ? List.of() : items;
        int resolvedPage = pageable == null ? 0 : Math.max(pageable.getPageNumber(), 0);
        int resolvedSize = resolveSize(pageable == null ? null : pageable.getPageSize());
        int total = source.size();
        int fromIndex = Math.min(resolvedPage * resolvedSize, total);
        int toIndex = Math.min(fromIndex + resolvedSize, total);

        return PageResult.of(source.subList(fromIndex, toIndex), resolvedPage, resolvedSize, total);
    }

    private static int resolveSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }
}
