package com.neobrutalism.crm.application.migration.validation.impl;

import com.neobrutalism.crm.application.migration.dto.HSBGCifDTO;
import com.neobrutalism.crm.application.migration.validation.SheetValidator;
import com.neobrutalism.crm.application.migration.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Validator for HSBG_theo_CIF sheet
 * Implements all business rules CT1-CT5
 */
@Slf4j
@Component
public class HSBGCifValidator implements SheetValidator<HSBGCifDTO> {
    
    private static final Set<String> VALID_DOCUMENT_FLOWS = Set.of("HSTD thường");
    private static final Set<String> VALID_CREDIT_TERM_CATEGORIES = Set.of("Vĩnh viễn");
    private static final Set<String> VALID_DOCUMENT_TYPES = Set.of(
        "PASS TTN", "SCF VEERFIN", "Trình cấp TD không qua CPC", 
        "Hồ sơ mở TKTT nhưng không giải ngân"
    );
    
    @Override
    public ValidationResult validate(HSBGCifDTO dto, int rowNumber) {
        ValidationResult result = new ValidationResult();
        
        // CT1: Check duplicate (will be checked in post-validation)
        // Key: Số CIF + Ngày giải ngân + Loại HS
        
        // CT2: Validate "Luồng hồ sơ" = "HSTD thường"
        if (dto.getDocumentFlow() != null && 
            !VALID_DOCUMENT_FLOWS.contains(dto.getDocumentFlow())) {
            result.addError("INVALID_DOCUMENT_FLOW",
                "Luồng hồ sơ phải là 'HSTD thường'",
                "documentFlow");
        }
        
        // CT3: Validate "Phân hạn cấp TD" = "Vĩnh viễn"
        if (dto.getCreditTermCategory() != null && 
            !VALID_CREDIT_TERM_CATEGORIES.contains(dto.getCreditTermCategory())) {
            result.addError("INVALID_CREDIT_TERM_CATEGORY",
                "Phân hạn cấp TD phải là 'Vĩnh viễn'",
                "creditTermCategory");
        }
        
        // CT4: Validate "Loại hồ sơ"
        if (dto.getDocumentType() != null && 
            !VALID_DOCUMENT_TYPES.contains(dto.getDocumentType())) {
            result.addError("INVALID_DOCUMENT_TYPE",
                "Loại hồ sơ không nằm trong danh sách cho phép",
                "documentType");
        }
        
        // CT5: Validate "Mã thùng" format
        if (dto.getBoxCode() != null && !dto.getBoxCode().isEmpty()) {
            if (!isValidBoxCode(dto.getBoxCode())) {
                result.addError("INVALID_BOX_CODE",
                    "Mã thùng không đúng format. Chỉ được chứa chữ in hoa, số và dấu gạch dưới (_)",
                    "boxCode");
            }
        }
        
        // Validate required fields
        if (dto.getCustomerCif() == null || dto.getCustomerCif().trim().isEmpty()) {
            result.addError("MISSING_REQUIRED_FIELD",
                "Số CIF khách hàng là bắt buộc",
                "customerCif");
        }
        
        if (dto.getDisbursementDate() == null) {
            result.addError("MISSING_REQUIRED_FIELD",
                "Ngày giải ngân là bắt buộc",
                "disbursementDate");
        }
        
        if (dto.getDocumentType() == null || dto.getDocumentType().trim().isEmpty()) {
            result.addError("MISSING_REQUIRED_FIELD",
                "Loại hồ sơ là bắt buộc",
                "documentType");
        }
        
        return result;
    }
    
    private boolean isValidBoxCode(String boxCode) {
        return boxCode.matches("^[A-Z0-9_]+$");
    }
    
    @Override
    public String getSheetType() {
        return "HSBG_theo_CIF";
    }
}

