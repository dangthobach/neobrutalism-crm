package com.neobrutalism.crm.domain.user.model;

/**
 * Data Scope - Phạm vi dữ liệu mà user có thể truy cập
 */
public enum DataScope {
    /**
     * ALL_BRANCHES - Xem tất cả branches
     * Dành cho Management role
     */
    ALL_BRANCHES,

    /**
     * CURRENT_BRANCH - Chỉ xem branch hiện tại và các branch con
     * Dành cho ORC (Operation Risk Control) role
     */
    CURRENT_BRANCH,

    /**
     * SELF_ONLY - Chỉ xem bản ghi của chính mình
     * Dành cho Maker/Checker role
     */
    SELF_ONLY
}
