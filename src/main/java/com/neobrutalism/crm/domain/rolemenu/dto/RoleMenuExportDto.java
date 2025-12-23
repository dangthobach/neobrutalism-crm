package com.neobrutalism.crm.domain.rolemenu.dto;

import com.neobrutalism.crm.utils.ExcelColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for exporting RoleMenu permissions to Excel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleMenuExportDto {

    @ExcelColumn(name = "Role Code", index = 0)
    private String roleCode;

    @ExcelColumn(name = "Role Name", index = 1)
    private String roleName;

    @ExcelColumn(name = "Menu Code", index = 2)
    private String menuCode;

    @ExcelColumn(name = "Menu Name", index = 3)
    private String menuName;

    @ExcelColumn(name = "Menu Path", index = 4)
    private String menuPath;

    @ExcelColumn(name = "Can View", index = 5)
    private String canView;

    @ExcelColumn(name = "Can Create", index = 6)
    private String canCreate;

    @ExcelColumn(name = "Can Edit", index = 7)
    private String canEdit;

    @ExcelColumn(name = "Can Delete", index = 8)
    private String canDelete;

    @ExcelColumn(name = "Can Export", index = 9)
    private String canExport;

    @ExcelColumn(name = "Can Import", index = 10)
    private String canImport;

    @ExcelColumn(name = "Granted Permissions", index = 11)
    private String grantedPermissions;

    /**
     * Convert boolean to readable string
     */
    public static String booleanToString(Boolean value) {
        if (value == null) {
            return "No";
        }
        return value ? "Yes" : "No";
    }

    /**
     * Build granted permissions summary
     */
    public static String buildGrantedPermissions(Boolean canView, Boolean canCreate,
                                                  Boolean canEdit, Boolean canDelete,
                                                  Boolean canExport, Boolean canImport) {
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(canView)) sb.append("View, ");
        if (Boolean.TRUE.equals(canCreate)) sb.append("Create, ");
        if (Boolean.TRUE.equals(canEdit)) sb.append("Edit, ");
        if (Boolean.TRUE.equals(canDelete)) sb.append("Delete, ");
        if (Boolean.TRUE.equals(canExport)) sb.append("Export, ");
        if (Boolean.TRUE.equals(canImport)) sb.append("Import, ");

        String result = sb.toString();
        if (result.endsWith(", ")) {
            result = result.substring(0, result.length() - 2);
        }
        return result.isEmpty() ? "None" : result;
    }
}
