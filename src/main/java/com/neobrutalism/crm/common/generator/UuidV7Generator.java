package com.neobrutalism.crm.common.generator;

import com.github.f4b6a3.uuid.UuidCreator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;


/**
 * UUID v7 Generator - Time-ordered UUIDs for better performance
 *
 * Benefits:
 * - Time-ordered: Better for B-tree indexes
 * - Sequential: Improved INSERT performance
 * - Clustered: Better cache locality
 * - Unique: Suitable for distributed systems
 */
public class UuidV7Generator implements IdentifierGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
