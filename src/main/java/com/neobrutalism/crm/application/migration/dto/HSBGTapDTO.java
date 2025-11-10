package com.neobrutalism.crm.application.migration.dto;

import com.neobrutalism.crm.utils.ExcelColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for HSBG_theo_tap sheet
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HSBGTapDTO {
    
    @ExcelColumn(name = "Kho VPBank")
    private String warehouseVpbank;
    
    @ExcelColumn(name = "Mã đơn vị")
    private String unitCode;
    
    @ExcelColumn(name = "Trách nhiệm bàn giao")
    private String deliveryResponsibility;
    
    @ExcelColumn(name = "Tháng phát sinh")
    private LocalDate occurrenceMonth;
    
    @ExcelColumn(name = "Tên tập")
    private String volumeName;
    
    @ExcelColumn(name = "Số lượng tập")
    private Integer volumeQuantity;
    
    @ExcelColumn(name = "Ngày phải bàn giao")
    private LocalDate requiredDeliveryDate;
    
    @ExcelColumn(name = "Ngày bàn giao")
    private LocalDate deliveryDate;
    
    @ExcelColumn(name = "Loại hồ sơ")
    private String documentType;
    
    @ExcelColumn(name = "Luồng hồ sơ")
    private String documentFlow;
    
    @ExcelColumn(name = "Phân hạn cấp TD")
    private String creditTermCategory;
    
    @ExcelColumn(name = "Ngày dự kiến tiêu hủy")
    private LocalDate expectedDestructionDate;
    
    @ExcelColumn(name = "Sản phẩm")
    private String product;
    
    @ExcelColumn(name = "Trạng thái case PDM")
    private String pdmCaseStatus;
    
    @ExcelColumn(name = "Ghi chú")
    private String notes;
    
    @ExcelColumn(name = "Mã thùng")
    private String boxCode;
    
    @ExcelColumn(name = "Ngày nhập kho VPBank")
    private LocalDate vpbankWarehouseEntryDate;
    
    @ExcelColumn(name = "Ngày chuyển kho Crown")
    private LocalDate crownWarehouseTransferDate;
    
    @ExcelColumn(name = "Khu vực")
    private String area;
    
    @ExcelColumn(name = "Hàng")
    private String row;
    
    @ExcelColumn(name = "Cột")
    private String column;
    
    @ExcelColumn(name = "Tình trạng thùng")
    private String boxCondition;
    
    @ExcelColumn(name = "Trạng thái thùng")
    private String boxStatus;
}

