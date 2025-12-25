package com.neobrutalism.crm.application.migration.service;

import com.neobrutalism.crm.application.migration.dto.HSBGCifDTO;
import com.neobrutalism.crm.application.migration.dto.HSBGHopDongDTO;
import com.neobrutalism.crm.application.migration.dto.HSBGTapDTO;
import com.neobrutalism.crm.application.migration.validation.impl.HSBGHopDongValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Service for normalizing and cleaning Excel data
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataNormalizer {
    
    private final HSBGHopDongValidator hopDongValidator;
    
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy")
    };
    
    /**
     * Normalize HSBG_theo_hop_dong DTO
     */
    public HSBGHopDongDTO normalizeHopDong(HSBGHopDongDTO dto) {
        if (dto == null) {
            return null;
        }
        
        // Normalize text fields (trim, uppercase where needed)
        dto.setBoxCode(normalizeBoxCode(dto.getBoxCode()));
        dto.setContractNumber(normalizeContractNumber(dto.getContractNumber()));
        dto.setCustomerCifCccdCmt(normalizeCif(dto.getCustomerCifCccdCmt()));
        dto.setCustomerName(normalizeCustomerName(dto.getCustomerName()));
        
        // Normalize dates
        dto.setRequiredDeliveryDate(normalizeDate(dto.getRequiredDeliveryDate()));
        dto.setDeliveryDate(normalizeDate(dto.getDeliveryDate()));
        dto.setDisbursementDate(normalizeDate(dto.getDisbursementDate()));
        dto.setDueDate(normalizeDate(dto.getDueDate()));
        dto.setVpbankWarehouseEntryDate(normalizeDate(dto.getVpbankWarehouseEntryDate()));
        dto.setCrownWarehouseTransferDate(normalizeDate(dto.getCrownWarehouseTransferDate()));
        
        // Calculate derived fields
        dto.setExpectedDestructionDate(
            hopDongValidator.calculateExpectedDestructionDate(dto)
        );
        
        dto.setCreditTermMonths(
            hopDongValidator.calculateCreditTermMonths(dto)
        );
        
        return dto;
    }
    
    /**
     * Normalize HSBG_theo_CIF DTO
     */
    public HSBGCifDTO normalizeCif(HSBGCifDTO dto) {
        if (dto == null) {
            return null;
        }
        
        dto.setBoxCode(normalizeBoxCode(dto.getBoxCode()));
        dto.setCustomerCif(normalizeCif(dto.getCustomerCif()));
        dto.setCustomerName(normalizeCustomerName(dto.getCustomerName()));
        
        dto.setRequiredDeliveryDate(normalizeDate(dto.getRequiredDeliveryDate()));
        dto.setDeliveryDate(normalizeDate(dto.getDeliveryDate()));
        dto.setDisbursementDate(normalizeDate(dto.getDisbursementDate()));
        dto.setVpbankWarehouseEntryDate(normalizeDate(dto.getVpbankWarehouseEntryDate()));
        dto.setCrownWarehouseTransferDate(normalizeDate(dto.getCrownWarehouseTransferDate()));
        
        return dto;
    }
    
    /**
     * Normalize HSBG_theo_tap DTO
     */
    public HSBGTapDTO normalizeTap(HSBGTapDTO dto) {
        if (dto == null) {
            return null;
        }
        
        dto.setBoxCode(normalizeBoxCode(dto.getBoxCode()));
        
        dto.setOccurrenceMonth(normalizeDate(dto.getOccurrenceMonth()));
        dto.setRequiredDeliveryDate(normalizeDate(dto.getRequiredDeliveryDate()));
        dto.setDeliveryDate(normalizeDate(dto.getDeliveryDate()));
        dto.setExpectedDestructionDate(normalizeDate(dto.getExpectedDestructionDate()));
        dto.setVpbankWarehouseEntryDate(normalizeDate(dto.getVpbankWarehouseEntryDate()));
        dto.setCrownWarehouseTransferDate(normalizeDate(dto.getCrownWarehouseTransferDate()));
        
        // Set expected destruction date to 31-Dec-9999 for this sheet type
        if (dto.getExpectedDestructionDate() == null) {
            dto.setExpectedDestructionDate(LocalDate.of(9999, 12, 31));
        }
        
        return dto;
    }
    
    /**
     * Normalize box code: uppercase, remove spaces, validate format
     */
    private String normalizeBoxCode(String boxCode) {
        if (boxCode == null || boxCode.trim().isEmpty()) {
            return null;
        }
        
        String normalized = boxCode.trim().toUpperCase().replaceAll("\\s+", "");
        
        // Validate format (only A-Z, 0-9, _)
        if (!normalized.matches("^[A-Z0-9_]+$")) {
            log.warn("Invalid box code format: {}", boxCode);
            return normalized; // Return anyway, validation will catch it
        }
        
        return normalized;
    }
    
    /**
     * Normalize contract number: trim, uppercase
     */
    private String normalizeContractNumber(String contractNumber) {
        if (contractNumber == null) {
            return null;
        }
        return contractNumber.trim().toUpperCase();
    }
    
    /**
     * Normalize CIF: trim, remove spaces
     */
    private String normalizeCif(String cif) {
        if (cif == null) {
            return null;
        }
        return cif.trim().replaceAll("\\s+", "");
    }
    
    /**
     * Normalize customer name: trim, proper case
     */
    private String normalizeCustomerName(String name) {
        if (name == null) {
            return null;
        }
        return name.trim();
    }
    
    /**
     * Normalize date: handle various formats
     */
    private LocalDate normalizeDate(LocalDate date) {
        // If already a LocalDate, return as-is
        return date;
    }
    
    /**
     * Parse date string with multiple formats
     */
    public LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = dateStr.trim();
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        
        log.warn("Could not parse date: {}", dateStr);
        return null;
    }
    
    /**
     * Generate duplicate key for duplicate detection
     */
    public String generateDuplicateKey(HSBGHopDongDTO dto) {
        // Key: Số HD + Loại HS + (Ngày giải ngân or Số CIF or other based on Loại HS)
        StringBuilder key = new StringBuilder();
        key.append(dto.getContractNumber()).append("|");
        key.append(dto.getDocumentType()).append("|");
        
        // Based on business rule CT2
        if (isDuplicateByDisbursementDate(dto.getDocumentType())) {
            key.append(dto.getDisbursementDate());
        } else if (isDuplicateByCif(dto.getDocumentType())) {
            key.append(dto.getCustomerCifCccdCmt());
        } else if ("TTK".equals(dto.getDocumentType())) {
            key.append(dto.getUnitCode()).append("|");
            key.append(dto.getCustomerCifCccdCmt()).append("|");
            key.append(dto.getDisbursementDate());
        }
        
        return key.toString();
    }
    
    public String generateDuplicateKey(HSBGCifDTO dto) {
        // Key: Số CIF + Ngày giải ngân + Loại HS
        return String.format("%s|%s|%s",
            dto.getCustomerCif(),
            dto.getDisbursementDate(),
            dto.getDocumentType()
        );
    }
    
    public String generateDuplicateKey(HSBGTapDTO dto) {
        // Key: Mã DV + TNBG + Tháng phát sinh + Sản phẩm
        return String.format("%s|%s|%s|%s",
            dto.getUnitCode(),
            dto.getDeliveryResponsibility(),
            dto.getOccurrenceMonth(),
            dto.getProduct()
        );
    }
    
    private boolean isDuplicateByDisbursementDate(String documentType) {
        return "LD".equals(documentType) || 
               "MD".equals(documentType) || 
               "OD".equals(documentType) || 
               "HDHM".equals(documentType) || 
               "KSSV".equals(documentType) ||
               "Bao thanh toán".equals(documentType) ||
               "Biên nhận thế chấp".equals(documentType);
    }
    
    private boolean isDuplicateByCif(String documentType) {
        return "CC".equals(documentType) || "TSBD".equals(documentType);
    }
}

