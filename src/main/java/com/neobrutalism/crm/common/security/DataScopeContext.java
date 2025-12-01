package com.neobrutalism.crm.common.security;

import com.neobrutalism.crm.common.multitenancy.TenantContext;
import com.neobrutalism.crm.domain.user.model.DataScope;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

/**
 * Data Scope Context - Lưu trữ thông tin về phạm vi dữ liệu mà user có thể truy cập
 * Thread-safe storage
 */
@Getter
@Builder
public class DataScopeContext {

    private static final ThreadLocal<DataScopeContext> CONTEXT = new ThreadLocal<>();

    private UUID userId;
    private String tenantId;
    private DataScope dataScope;
    private UUID branchId;
    private Set<UUID> accessibleBranchIds; // Các branch IDs mà user có thể truy cập

    public static void set(DataScopeContext context) {
        CONTEXT.set(context);
    }

    public static DataScopeContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * Kiểm tra xem có data scope context không
     */
    public static boolean hasContext() {
        return CONTEXT.get() != null;
    }

    /**
     * Lấy user ID từ context
     */
    public static UUID getCurrentUserId() {
        DataScopeContext context = get();
        return context != null ? context.getUserId() : null;
    }

    /**
     * Lấy tenant ID từ context
     */
    public static String getCurrentTenantId() {
        DataScopeContext context = get();
        return context != null ? context.getTenantId() : TenantContext.getCurrentTenant();
    }

    /**
     * Lấy data scope từ context
     */
    public static DataScope getCurrentDataScope() {
        DataScopeContext context = get();
        return context != null ? context.getDataScope() : DataScope.SELF_ONLY;
    }

    /**
     * Lấy branch ID từ context
     */
    public static UUID getCurrentBranchId() {
        DataScopeContext context = get();
        return context != null ? context.getBranchId() : null;
    }

    /**
     * Lấy accessible branch IDs từ context
     */
    public static Set<UUID> getAccessibleBranchIds() {
        DataScopeContext context = get();
        return context != null ? context.getAccessibleBranchIds() : Set.of();
    }
}
