package com.neobrutalism.crm.common.security.annotation;

import com.neobrutalism.crm.common.enums.PermissionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for method-level permission checking
 * Usage: @RequirePermission(resource = "USER", permission = PermissionType.WRITE)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * Resource/entity name to check permission for
     */
    String resource();

    /**
     * Permission type required (READ, WRITE, DELETE, EXECUTE)
     */
    PermissionType permission();

    /**
     * Optional: specific action for fine-grained control
     */
    String action() default "";

    /**
     * Whether to check tenant isolation
     */
    boolean checkTenant() default true;
}
