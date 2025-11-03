package com.neobrutalism.crm.domain.customer.dto;

import com.neobrutalism.crm.domain.customer.model.CustomerStatus;
import com.neobrutalism.crm.domain.customer.model.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Response DTO for customer statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatsResponse {
    private Long total;
    private Map<CustomerStatus, Long> byStatus;
    private Map<CustomerType, Long> byType;
    private Long vipCount;
    private BigDecimal averageRevenue;
}
