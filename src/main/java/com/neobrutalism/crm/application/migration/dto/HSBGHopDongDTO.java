package com.neobrutalism.crm.application.migration.dto;

import com.neobrutalism.crm.utils.ExcelColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for HSBG_theo_hop_dong sheet
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HSBGHopDongDTO {
    
    @ExcelColumn(name = "Kho VPBank")
    private String warehouseVpbank;
    
    @ExcelColumn(name = "Mã đơn vị")
    private String unitCode;
    
    @ExcelColumn(name = "Trách nhiệm bàn giao")
    private String deliveryResponsibility;
    
    @ExcelColumn(name = "Số hợp đồng")
    private String contractNumber;
    
    @ExcelColumn(name = "Tên tập")
    private String volumeName;
    
    @ExcelColumn(name = "Số lượng tập")
    private Integer volumeQuantity;
    
    @ExcelColumn(name = "Số CIF/ CCCD/ CMT khách hàng")
    private String customerCifCccdCmt;
    
    @ExcelColumn(name = "Tên khách hàng")
    private String customerName;
    
    @ExcelColumn(name = "Phân khúc khách hàng")
    private String customerSegment;
    
    @ExcelColumn(name = "Ngày phải bàn giao")
    private LocalDate requiredDeliveryDate;
    
    @ExcelColumn(name = "Ngày bàn giao")
    private LocalDate deliveryDate;
    
    @ExcelColumn(name = "Ngày giải ngân")
    private LocalDate disbursementDate;
    
    @ExcelColumn(name = "Ngày đến hạn")
    private LocalDate dueDate;
    
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
    
    @ExcelColumn(name = "Thời hạn cấp TD")
    private Integer creditTermMonths;
    
    @ExcelColumn(name = "Mã DAO")
    private String daoCode;
    
    @ExcelColumn(name = "Mã TS")
    private String tsCode;
    
    @ExcelColumn(name = "RRT.ID")
    private String rrtId;
    
    @ExcelColumn(name = "Mã NQ")
    private String nqCode;
}

