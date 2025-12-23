package com.neobrutalism.crm.application.migration.validation.impl;

import com.neobrutalism.crm.application.migration.dto.HSBGHopDongDTO;
import com.neobrutalism.crm.application.migration.validation.SheetValidator;
import com.neobrutalism.crm.application.migration.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Validator for HSBG_theo_hop_dong sheet
 * Implements all business rules CT1-CT8
 */
@Slf4j
@Component
public class HSBGHopDongValidator implements SheetValidator<HSBGHopDongDTO> {
    
    private static final Set<String> VALID_DOCUMENT_TYPES = Set.of(
        "LD", "MD", "CC", "OD", "TTK", "HDHM", "TSBD", 
        "KSSV", "Bao thanh toán", "Biên nhận thế chấp"
    );
    
    private static final Set<String> VALID_CREDIT_TERM_CATEGORIES = Set.of(
        "Vĩnh viễn", "Ngắn hạn", "Trung hạn", "Dài hạn"
    );
    
    @Override
    public ValidationResult validate(HSBGHopDongDTO dto, int rowNumber) {
        ValidationResult result = new ValidationResult();
        
        // CT1: Calculate "Ngày dự kiến tiêu hủy" (will be set by normalizer)
        // This is calculated, not validated here
        
        // CT2: Check duplicate by Loại hồ sơ (will be checked in post-validation)
        // This is done after all records are loaded
        
        // CT3: Validate "Phân hạn cấp TD"
        if (dto.getCreditTermCategory() != null && 
            !VALID_CREDIT_TERM_CATEGORIES.contains(dto.getCreditTermCategory())) {
            result.addError("INVALID_CREDIT_TERM_CATEGORY", 
                "Phân hạn cấp TD không hợp lệ. Phải là: Vĩnh viễn, Ngắn hạn, Trung hạn, hoặc Dài hạn",
                "creditTermCategory");
        }
        
        // CT4: Validate "Loại hồ sơ"
        if (dto.getDocumentType() != null && 
            !VALID_DOCUMENT_TYPES.contains(dto.getDocumentType())) {
            result.addError("INVALID_DOCUMENT_TYPE",
                "Loại hồ sơ không nằm trong danh sách cho phép",
                "documentType");
        }
        
        // CT5: Validate "Thời hạn cấp TD" (must be positive integer if provided)
        if (dto.getCreditTermMonths() != null) {
            if (dto.getCreditTermMonths() <= 0) {
                result.addError("INVALID_CREDIT_TERM_MONTHS",
                    "Thời hạn cấp TD phải là số nguyên dương",
                    "creditTermMonths");
            }
        }
        
        // CT6: Validate "Ngày đến hạn tiêu hủy" according to Phân hạn cấp TD
        if ("Vĩnh viễn".equals(dto.getCreditTermCategory())) {
            if (dto.getExpectedDestructionDate() == null) {
                result.addError("MISSING_EXPECTED_DESTRUCTION_DATE",
                    "Ngày dự kiến tiêu hủy bắt buộc khi Phân hạn cấp TD = Vĩnh viễn",
                    "expectedDestructionDate");
            } else if (dto.getExpectedDestructionDate().getYear() != 9999) {
                result.addError("INVALID_EXPECTED_DESTRUCTION_DATE",
                    "Ngày dự kiến tiêu hủy phải là 31-Dec-9999 khi Phân hạn cấp TD = Vĩnh viễn",
                    "expectedDestructionDate");
            }
        }
        
        // CT7: Validate "Ngày đến hạn tiêu hủy" according to Ngày đến hạn
        if (dto.getDueDate() == null) {
            if (dto.getExpectedDestructionDate() == null) {
                result.addError("MISSING_EXPECTED_DESTRUCTION_DATE",
                    "Ngày dự kiến tiêu hủy bắt buộc khi Ngày đến hạn trống",
                    "expectedDestructionDate");
            } else if (dto.getExpectedDestructionDate().getYear() != 9999) {
                result.addError("INVALID_EXPECTED_DESTRUCTION_DATE",
                    "Ngày dự kiến tiêu hủy phải là 31-Dec-9999 khi Ngày đến hạn trống",
                    "expectedDestructionDate");
            }
        }
        
        // CT8: Validate "Mã thùng" format
        if (dto.getBoxCode() != null && !dto.getBoxCode().isEmpty()) {
            if (!isValidBoxCode(dto.getBoxCode())) {
                result.addError("INVALID_BOX_CODE",
                    "Mã thùng không đúng format. Chỉ được chứa chữ in hoa, số và dấu gạch dưới (_)",
                    "boxCode");
            }
        }
        
        // Validate required fields (marked in blue)
        if (dto.getContractNumber() == null || dto.getContractNumber().trim().isEmpty()) {
            result.addError("MISSING_REQUIRED_FIELD",
                "Số hợp đồng là bắt buộc",
                "contractNumber");
        }
        
        if (dto.getDocumentType() == null || dto.getDocumentType().trim().isEmpty()) {
            result.addError("MISSING_REQUIRED_FIELD",
                "Loại hồ sơ là bắt buộc",
                "documentType");
        }
        
        return result;
    }
    
    /**
     * Validate box code format: only uppercase letters, numbers, and underscore
     */
    private boolean isValidBoxCode(String boxCode) {
        return boxCode.matches("^[A-Z0-9_]+$");
    }
    
    /**
     * Calculate expected destruction date based on business rules
     */
    public LocalDate calculateExpectedDestructionDate(HSBGHopDongDTO dto) {
        if ("Vĩnh viễn".equals(dto.getCreditTermCategory())) {
            return LocalDate.of(9999, 12, 31);
        }
        
        if (dto.getDueDate() == null) {
            return LocalDate.of(9999, 12, 31);
        }
        
        return switch (dto.getCreditTermCategory()) {
            case "Ngắn hạn" -> dto.getDueDate().plusMonths(12 * 5);
            case "Trung hạn" -> dto.getDueDate().plusMonths(12 * 10);
            case "Dài hạn" -> dto.getDueDate().plusMonths(12 * 15);
            default -> LocalDate.of(9999, 12, 31);
        };
    }
    
    /**
     * Calculate credit term months based on business rules
     */
    public Integer calculateCreditTermMonths(HSBGHopDongDTO dto) {
        if (dto.getDueDate() == null) {
            return null;
        }
        if (dto.getDisbursementDate() == null) {
            return null;
        }
        
        long months = ChronoUnit.MONTHS.between(
            dto.getDisbursementDate(), 
            dto.getDueDate()
        );
        
        return Math.max(1, (int) months);
    }
    
    @Override
    public String getSheetType() {
        return "HSBG_theo_hop_dong";
    }
}

