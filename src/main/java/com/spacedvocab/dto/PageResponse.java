package com.spacedvocab.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Stable JSON shape for paged lists. {@code page} is 0-based. */
public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
