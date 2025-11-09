package com.neobrutalism.crm.application.migration.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Staging entity for HSBG_theo_CIF sheet
 */
@Entity
@Table(name = "staging_hsbg_cif")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StagingHSBGCif {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "job_id", nullable = false)
    private UUID jobId;
    
    @Column(name = "sheet_id", nullable = false)
    private UUID sheetId;
    
    @Column(name = "row_number", nullable = false)
    private Long rowNumber;
    
    // Data fields
    @Column(name = "kho_vpbank")
    private String warehouseVpbank;
    
    @Column(name = "ma_don_vi")
    private String unitCode;
    
    @Column(name = "trach_nhiem_ban_giao")
    private String deliveryResponsibility;
    
    @Column(name = "so_cif")
    private String customerCif;
    
    @Column(name = "ten_khach_hang")
    private String customerName;
    
    @Column(name = "ten_tap")
    private String volumeName;
    
    @Column(name = "so_luong_tap")
    private Integer volumeQuantity;
    
    @Column(name = "phan_khach_khach_hang")
    private String customerSegment;
    
    @Column(name = "ngay_phai_ban_giao")
    private LocalDate requiredDeliveryDate;
    
    @Column(name = "ngay_ban_giao")
    private LocalDate deliveryDate;
    
    @Column(name = "ngay_giai_ngan")
    private LocalDate disbursementDate;
    
    @Column(name = "loai_ho_so")
    private String documentType;
    
    @Column(name = "luong_ho_so")
    private String documentFlow;
    
    @Column(name = "phan_han_cap_td")
    private String creditTermCategory;
    
    @Column(name = "san_pham")
    private String product;
    
    @Column(name = "trang_thai_case_pdm")
    private String pdmCaseStatus;
    
    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "ma_nq")
    private String nqCode;
    
    @Column(name = "ma_thung")
    private String boxCode;
    
    @Column(name = "ngay_nhap_kho_vpbank")
    private LocalDate vpbankWarehouseEntryDate;
    
    @Column(name = "ngay_chuyen_kho_crown")
    private LocalDate crownWarehouseTransferDate;
    
    @Column(name = "khu_vuc")
    private String area;
    
    @Column(name = "hang")
    private String row;
    
    @Column(name = "cot")
    private String column;
    
    @Column(name = "tinh_trang_thung")
    private String boxCondition;
    
    @Column(name = "trang_thai_thung")
    private String boxStatus;
    
    // Processing fields
    @Column(name = "validation_status", nullable = false, length = 50)
    @Builder.Default
    private String validationStatus = "PENDING";
    
    @Column(name = "validation_errors", columnDefinition = "JSONB")
    private String validationErrors;
    
    @Column(name = "normalized_data", columnDefinition = "JSONB")
    private String normalizedData;
    
    @Column(name = "duplicate_key", length = 500)
    private String duplicateKey;
    
    @Column(name = "is_duplicate", nullable = false)
    @Builder.Default
    private Boolean isDuplicate = false;
    
    @Column(name = "master_data_exists")
    private Boolean masterDataExists;
    
    @Column(name = "inserted_to_master", nullable = false)
    @Builder.Default
    private Boolean insertedToMaster = false;
    
    @Column(name = "inserted_at")
    private java.time.Instant insertedAt;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private java.time.Instant createdAt = java.time.Instant.now();
}

