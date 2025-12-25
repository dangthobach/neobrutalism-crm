package com.neobrutalism.crm.application.migration.validation.impl;

import com.neobrutalism.crm.application.migration.dto.HSBGTapDTO;
import com.neobrutalism.crm.application.migration.validation.SheetValidator;
import com.neobrutalism.crm.application.migration.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

/**
 * Validator for HSBG_theo_tap sheet
 * Implements all business rules CT1-CT7
 */
@Slf4j
@Component
public class HSBGTapValidator implements SheetValidator<HSBGTapDTO> {
    
    private static final Set<String> VALID_DOCUMENT_TYPES = Set.of("KSSV");
    private static final Set<String> VALID_DOCUMENT_FLOWS = Set.of("HSTD thường");
    private static final Set<String> VALID_CREDIT_TERM_CATEGORIES = Set.of("Vĩnh viễn");
    private static final Set<String> VALID_PRODUCTS = Set.of("KSSV");
    private static final LocalDate EXPECTED_DESTRUCTION_DATE = LocalDate.of(9999, 12, 31);
    
    @Override
    public ValidationResult validate(HSBGTapDTO dto, int rowNumber) {
        ValidationResult result = new ValidationResult();
        
        // CT1: Check duplicate (will be checked in post-validation)
        // Key: Mã DV + TNBG + Tháng phát sinh + Sản phẩm
        
        // CT2: Validate "Loại hồ sơ" = "KSSV"
        if (dto.getDocumentType() != null && 
            !VALID_DOCUMENT_TYPES.contains(dto.getDocumentType())) {
            result.addError("INVALID_DOCUMENT_TYPE",
                "Loại hồ sơ phải là 'KSSV'",
                "documentType");
        }
        
        // CT3: Validate "Luồng hồ sơ" = "HSTD thường"
        if (dto.getDocumentFlow() != null && 
            !VALID_DOCUMENT_FLOWS.contains(dto.getDocumentFlow())) {
            result.addError("INVALID_DOCUMENT_FLOW",
                "Luồng hồ sơ phải là 'HSTD thường'",
                "documentFlow");
        }
        
        // CT4: Validate "Phân hạn cấp TD" = "Vĩnh viễn"
        if (dto.getCreditTermCategory() != null && 
            !VALID_CREDIT_TERM_CATEGORIES.contains(dto.getCreditTermCategory())) {
            result.addError("INVALID_CREDIT_TERM_CATEGORY",
                "Phân hạn cấp TD phải là 'Vĩnh viễn'",
                "creditTermCategory");
        }
        
        // CT5: Validate "Ngày dự kiến tiêu hủy" = "31-Dec-9999"
        if (dto.getExpectedDestructionDate() != null && 
            !EXPECTED_DESTRUCTION_DATE.equals(dto.getExpectedDestructionDate())) {
            result.addError("INVALID_EXPECTED_DESTRUCTION_DATE",
                "Ngày dự kiến tiêu hủy phải là 31-Dec-9999",
                "expectedDestructionDate");
        }
        
        // CT6: Validate "Sản phẩm" = "KSSV"
        if (dto.getProduct() != null && 
            !VALID_PRODUCTS.contains(dto.getProduct())) {
            result.addError("INVALID_PRODUCT",
                "Sản phẩm phải là 'KSSV'",
                "product");
        }
        
        // CT7: Validate "Mã thùng" format
        if (dto.getBoxCode() != null && !dto.getBoxCode().isEmpty()) {
            if (!isValidBoxCode(dto.getBoxCode())) {
                result.addError("INVALID_BOX_CODE",
                    "Mã thùng không đúng format. Chỉ được chứa chữ in hoa, số và dấu gạch dưới (_)",
                    "boxCode");
            }
        }
        
        // Validate required fields
        if (dto.getUnitCode() == null || dto.getUnitCode().trim().isEmpty()) {
            result.addError("MISSING_REQUIRED_FIELD",
                "Mã đơn vị là bắt buộc",
                "unitCode");
        }
        
        if (dto.getDeliveryResponsibility() == null || dto.getDeliveryResponsibility().trim().isEmpty()) {
            result.addError("MISSING_REQUIRED_FIELD",
                "Trách nhiệm bàn giao là bắt buộc",
                "deliveryResponsibility");
        }
        
        if (dto.getOccurrenceMonth() == null) {
            result.addError("MISSING_REQUIRED_FIELD",
                "Tháng phát sinh là bắt buộc",
                "occurrenceMonth");
        }
        
        if (dto.getProduct() == null || dto.getProduct().trim().isEmpty()) {
            result.addError("MISSING_REQUIRED_FIELD",
                "Sản phẩm là bắt buộc",
                "product");
        }
        
        return result;
    }
    
    private boolean isValidBoxCode(String boxCode) {
        return boxCode.matches("^[A-Z0-9_]+$");
    }
    
    @Override
    public String getSheetType() {
        return "HSBG_theo_tap";
    }
}

