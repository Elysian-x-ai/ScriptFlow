package com.scriptflow.common.util;

import com.scriptflow.common.constant.GlobalConstants;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Pagination utility and DTO.
 */
@Data
public class PageUtils<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int page = 1;
    private int pageSize = GlobalConstants.PAGE_SIZE_DEFAULT;
    private int total;
    private List<T> records = Collections.emptyList();

    public PageUtils() {}

    public PageUtils(int page, int pageSize) {
        this.page = Math.max(page, 1);
        this.pageSize = Math.min(Math.max(pageSize, 1), GlobalConstants.PAGE_SIZE_MAX);
    }

    public long getOffset() {
        return (long) (page - 1) * pageSize;
    }

    public static <T> PageUtils<T> of(int page, int pageSize, int total, List<T> records) {
        PageUtils<T> p = new PageUtils<>(page, pageSize);
        p.total = total;
        p.records = records;
        return p;
    }
}
