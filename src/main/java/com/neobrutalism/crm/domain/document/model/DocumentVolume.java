package com.neobrutalism.crm.domain.document.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Master data entity for document volumes (TAP)
 * Receives transformed data from migration staging tables (Tap records)
 */
@Entity
@Table(name = "document_volumes", 
    indexes = {
        @Index(name = "idx_volume_name", columnList = "volume_name"),
        @Index(name = "idx_volume_tenant", columnList = "tenant_id"),
        @Index(name = "idx_volume_box_code", columnList = "box_code"),
        @Index(name = "idx_volume_customer_cif", columnList = "customer_cif")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVolume {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    // Core volume identity
    @Column(name = "volume_name", nullable = false, length = 200)
    private String volumeName;
    
    @Column(name = "volume_quantity")
    private Integer volumeQuantity;
    
    // Customer reference
    @Column(name = "customer_cif", length = 100)
    private String customerCif;
    
    @Column(name = "customer_name", length = 500)
    private String customerName;
    
    @Column(name = "customer_segment", length = 100)
    private String customerSegment;
    
    // Organization context
    @Column(name = "unit_code", length = 100)
    private String unitCode;
    
    @Column(name = "warehouse_vpbank", length = 100)
    private String warehouseVpbank;
    
    @Column(name = "delivery_responsibility", length = 200)
    private String deliveryResponsibility;
    
    // Document information
    @Column(name = "document_type", length = 100)
    private String documentType;
    
    @Column(name = "document_flow", length = 100)
    private String documentFlow;
    
    @Column(name = "credit_term_category", length = 100)
    private String creditTermCategory;
    
    // Important dates
    @Column(name = "required_delivery_date")
    private LocalDate requiredDeliveryDate;
    
    @Column(name = "delivery_date")
    private LocalDate deliveryDate;
    
    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;
    
    // Status
    @Column(name = "product", length = 200)
    private String product;
    
    @Column(name = "pdm_case_status", length = 100)
    private String pdmCaseStatus;
    
    // Physical storage
    @Column(name = "box_code", length = 100)
    private String boxCode;
    
    @Column(name = "vpbank_warehouse_entry_date")
    private LocalDate vpbankWarehouseEntryDate;
    
    @Column(name = "crown_warehouse_transfer_date")
    private LocalDate crownWarehouseTransferDate;
    
    @Column(name = "area", length = 50)
    private String area;
    
    @Column(name = "storage_row", length = 50)
    private String storageRow;

    @Column(name = "storage_column", length = 50)
    private String storageColumn;
    
    @Column(name = "box_condition", length = 100)
    private String boxCondition;
    
    @Column(name = "box_status", length = 100)
    private String boxStatus;
    
    // Additional codes
    @Column(name = "nq_code", length = 100)
    private String nqCode;
    
    // Metadata
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "source_system", length = 50)
    @Builder.Default
    private String sourceSystem = "MIGRATION";
    
    @Column(name = "migration_job_id")
    private UUID migrationJobId;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "deleted_at")
    private Instant deletedAt;
    
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
}
