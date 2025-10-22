package com.neobrutalism.crm.common.cqrs;

import java.lang.annotation.*;

/**
 * Marker annotation for Read Model entities
 * Read models are denormalized, optimized for queries
 * Should be immutable and updated only via event handlers
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReadModel {
    /**
     * The aggregate type this read model represents
     */
    String aggregate();

    /**
     * Description of this read model's purpose
     */
    String description() default "";
}
