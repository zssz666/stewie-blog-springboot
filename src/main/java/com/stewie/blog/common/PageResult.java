package com.stewie.blog.common;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 分页响应数据
 * <pre>
 * {
 *   "list": [...],
 *   "total": 100,
 *   "page": 1,
 *   "size": 10
 * }
 * </pre>
 */
@Data
public class PageResult<T> {

    private List<T> list;
    private long total;
    private long page;
    private long size;

    public PageResult() {
    }

    public PageResult(List<T> list, long total, long page, long size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PageResult<T> of(long page, long size, long total, List<T> list) {
        return new PageResult<>(list, total, page, size);
    }

    public static <T> PageResult<T> empty(long page, long size) {
        return new PageResult<>(Collections.emptyList(), 0L, page, size);
    }

    /**
     * 总页数
     */
    public long getPages() {
        if (size <= 0) {
            return 0L;
        }
        return (total + size - 1) / size;
    }
}
