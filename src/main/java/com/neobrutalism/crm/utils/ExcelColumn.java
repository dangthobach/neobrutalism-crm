package com.neobrutalism.crm.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelColumn {

    String name();
    String numberFormat() default "General";

    /**
     * Column index (0-based)
     */
    int index() default -1;

    // ========== VALIDATION ATTRIBUTES ==========

    /**
     * Có bắt buộc không
     */
    boolean required() default false;
    
    /**
     * Độ dài tối đa
     */
    int maxLength() default -1;
    
    /**
     * Pattern regex để validate
     */
    String pattern() default "";
    
    /**
     * Mô tả cột
     */
    String description() default "";
    
    /**
     * Ví dụ giá trị
     */
    String example() default "";
    
    /**
     * Vị trí cột (A, B, C, ...)
     */
    String position() default "";
}