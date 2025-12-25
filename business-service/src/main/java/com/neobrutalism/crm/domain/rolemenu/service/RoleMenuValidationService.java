package com.neobrutalism.crm.domain.rolemenu.service;

import com.neobrutalism.crm.domain.menu.model.Menu;
import com.neobrutalism.crm.domain.menu.repository.MenuRepository;
import com.neobrutalism.crm.domain.rolemenu.model.RoleMenu;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for validating RoleMenu permission dependencies and conflicts
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleMenuValidationService {

    private final MenuRepository menuRepository;

    /**
     * Validate RoleMenu before save
     * Checks for logical dependencies and potential conflicts
     */
    public ValidationResult validate(RoleMenu roleMenu) {
        ValidationResult result = new ValidationResult();

        // Check 1: If canEdit is true, canView should also be true
        if (Boolean.TRUE.equals(roleMenu.getCanEdit()) && !Boolean.TRUE.equals(roleMenu.getCanView())) {
            result.addWarning("Edit permission requires View permission. Consider enabling 'canView'.");
        }

        // Check 2: If canDelete is true, canView should also be true
        if (Boolean.TRUE.equals(roleMenu.getCanDelete()) && !Boolean.TRUE.equals(roleMenu.getCanView())) {
            result.addWarning("Delete permission requires View permission. Consider enabling 'canView'.");
        }

        // Check 3: If canCreate is true but canView is false, it might be intentional but unusual
        if (Boolean.TRUE.equals(roleMenu.getCanCreate()) && !Boolean.TRUE.equals(roleMenu.getCanView())) {
            result.addInfo("Create permission without View permission. User can create but not view existing items.");
        }

        // Check 4: If canExport is true, canView should also be true
        if (Boolean.TRUE.equals(roleMenu.getCanExport()) && !Boolean.TRUE.equals(roleMenu.getCanView())) {
            result.addWarning("Export permission requires View permission. Consider enabling 'canView'.");
        }

        // Check 5: If canImport is true, canCreate should also be true
        if (Boolean.TRUE.equals(roleMenu.getCanImport()) && !Boolean.TRUE.equals(roleMenu.getCanCreate())) {
            result.addWarning("Import permission typically requires Create permission. Consider enabling 'canCreate'.");
        }

        // Check 6: Verify menu exists
        if (roleMenu.getMenuId() != null) {
            Optional<Menu> menu = menuRepository.findById(roleMenu.getMenuId());
            if (menu.isEmpty()) {
                result.addError("Menu with ID " + roleMenu.getMenuId() + " does not exist.");
            }
        }

        // Check 7: Check if all permissions are false (no access granted)
        if (!Boolean.TRUE.equals(roleMenu.getCanView()) &&
            !Boolean.TRUE.equals(roleMenu.getCanCreate()) &&
            !Boolean.TRUE.equals(roleMenu.getCanEdit()) &&
            !Boolean.TRUE.equals(roleMenu.getCanDelete()) &&
            !Boolean.TRUE.equals(roleMenu.getCanExport()) &&
            !Boolean.TRUE.equals(roleMenu.getCanImport())) {
            result.addWarning("No permissions granted. This RoleMenu will have no effect.");
        }

        return result;
    }

    /**
     * Auto-fix common permission dependencies
     * Returns a new RoleMenu with auto-enabled dependencies
     */
    public RoleMenu autoFixDependencies(RoleMenu roleMenu) {
        RoleMenu fixed = new RoleMenu();
        fixed.setId(roleMenu.getId());
        fixed.setRoleId(roleMenu.getRoleId());
        fixed.setMenuId(roleMenu.getMenuId());

        // Start with original values
        Boolean canView = roleMenu.getCanView();
        Boolean canCreate = roleMenu.getCanCreate();
        Boolean canEdit = roleMenu.getCanEdit();
        Boolean canDelete = roleMenu.getCanDelete();
        Boolean canExport = roleMenu.getCanExport();
        Boolean canImport = roleMenu.getCanImport();

        // Auto-enable canView if any read-dependent permission is enabled
        if (Boolean.TRUE.equals(canEdit) ||
            Boolean.TRUE.equals(canDelete) ||
            Boolean.TRUE.equals(canExport)) {
            canView = true;
        }

        // Auto-enable canCreate if canImport is enabled
        if (Boolean.TRUE.equals(canImport)) {
            canCreate = true;
        }

        fixed.setCanView(canView);
        fixed.setCanCreate(canCreate);
        fixed.setCanEdit(canEdit);
        fixed.setCanDelete(canDelete);
        fixed.setCanExport(canExport);
        fixed.setCanImport(canImport);

        return fixed;
    }

    /**
     * Validate multiple RoleMenus for a role (batch validation)
     */
    public Map<UUID, ValidationResult> validateBatch(List<RoleMenu> roleMenus) {
        Map<UUID, ValidationResult> results = new HashMap<>();

        for (RoleMenu roleMenu : roleMenus) {
            ValidationResult result = validate(roleMenu);
            if (roleMenu.getId() != null) {
                results.put(roleMenu.getId(), result);
            }
        }

        return results;
    }

    /**
     * Get suggestions for fixing validation issues
     */
    public List<String> getSuggestions(ValidationResult validationResult) {
        List<String> suggestions = new ArrayList<>();

        for (String warning : validationResult.getWarnings()) {
            if (warning.contains("Edit") && warning.contains("View")) {
                suggestions.add("Enable 'canView' to allow viewing before editing");
            }
            if (warning.contains("Delete") && warning.contains("View")) {
                suggestions.add("Enable 'canView' to allow viewing before deleting");
            }
            if (warning.contains("Export") && warning.contains("View")) {
                suggestions.add("Enable 'canView' to allow viewing data to export");
            }
            if (warning.contains("Import") && warning.contains("Create")) {
                suggestions.add("Enable 'canCreate' to allow creating items from import");
            }
        }

        return suggestions;
    }

    /**
     * Validation Result class
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> info = new ArrayList<>();

        public void addError(String message) {
            errors.add(message);
        }

        public void addWarning(String message) {
            warnings.add(message);
        }

        public void addInfo(String message) {
            info.add(message);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public boolean hasInfo() {
            return !info.isEmpty();
        }

        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }

        public List<String> getInfo() {
            return Collections.unmodifiableList(info);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("valid", isValid());
            map.put("errors", errors);
            map.put("warnings", warnings);
            map.put("info", info);
            map.put("errorCount", errors.size());
            map.put("warningCount", warnings.size());
            map.put("infoCount", info.size());
            return map;
        }
    }
}
