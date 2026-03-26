package com.datastd.common.dto;

import java.util.List;
import java.util.Map;

/**
 * Generic paginated-result wrapper returned by endpoints that support
 * page / size query parameters.
 */
public class PagedResult {

    private List<Map<String, Object>> records;
    private int page;
    private int size;
    private long totalRecords;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public PagedResult() {}

    public PagedResult(List<Map<String, Object>> records, int page, int size,
                       long totalRecords, int totalPages,
                       boolean hasNext, boolean hasPrevious) {
        this.records = records;
        this.page = page;
        this.size = size;
        this.totalRecords = totalRecords;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }

    // ── Factory helper ──────────────────────────────────────────

    /**
     * Build a {@link PagedResult} by slicing an in-memory list.
     *
     * @param allRecords the full (unsorted) list of records
     * @param page       zero-based page index
     * @param size       maximum records per page
     */
    public static PagedResult of(List<Map<String, Object>> allRecords, int page, int size) {
        int total = (allRecords == null) ? 0 : allRecords.size();
        int totalPages = (total == 0) ? 0 : (int) Math.ceil((double) total / size);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<Map<String, Object>> slice;
        if (fromIndex >= total) {
            slice = List.of();
        } else {
            slice = allRecords.subList(fromIndex, toIndex);
        }

        return new PagedResult(
                slice,
                page,
                size,
                total,
                totalPages,
                page < totalPages - 1,
                page > 0
        );
    }

    // ── Getters & Setters ───────────────────────────────────────

    public List<Map<String, Object>> getRecords() { return records; }
    public void setRecords(List<Map<String, Object>> records) { this.records = records; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }

    public boolean isHasPrevious() { return hasPrevious; }
    public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
}

