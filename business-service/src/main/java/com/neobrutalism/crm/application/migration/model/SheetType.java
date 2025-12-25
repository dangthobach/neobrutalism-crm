package com.neobrutalism.crm.application.migration.model;

/**
 * Excel sheet types for HSBG migration
 */
public enum SheetType {
    HSBG_THEO_HOP_DONG("HSBG_theo_hop_dong"),
    HSBG_THEO_CIF("HSBG_theo_CIF"),
    HSBG_THEO_TAP("HSBG_theo_tap");
    
    private final String sheetName;
    
    SheetType(String sheetName) {
        this.sheetName = sheetName;
    }
    
    public String getSheetName() {
        return sheetName;
    }
    
    public static SheetType fromSheetName(String sheetName) {
        for (SheetType type : values()) {
            if (type.sheetName.equalsIgnoreCase(sheetName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown sheet name: " + sheetName);
    }
}

