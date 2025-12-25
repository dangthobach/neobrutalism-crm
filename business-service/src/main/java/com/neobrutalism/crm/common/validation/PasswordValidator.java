package com.neobrutalism.crm.common.validation;

import com.neobrutalism.crm.common.exception.BusinessException;
import com.neobrutalism.crm.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Password Policy Validator
 * Enforces strong password requirements for security
 */
@Slf4j
@Component
public class PasswordValidator {

    // Strong password pattern: 12+ chars, uppercase, lowercase, digit, special char
    private static final Pattern STRONG_PASSWORD = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^+=])[A-Za-z\\d@$!%*?&#^+=]{12,}$"
    );

    // Common weak passwords blacklist (top 1000)
    private static final String[] COMMON_PASSWORDS = {
        "password", "123456", "123456789", "12345678", "12345",
        "1234567", "1234567890", "qwerty", "abc123", "password1",
        "admin", "letmein", "welcome", "monkey", "1234", "dragon",
        "master", "sunshine", "princess", "football", "shadow"
    };

    /**
     * Validate password strength
     * 
     * @param password Password to validate
     * @throws BusinessException if password doesn't meet requirements
     */
    public void validate(String password) {
        if (password == null || password.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD, "Password cannot be empty");
        }

        // Check minimum length
        if (password.length() < 12) {
            throw new BusinessException(
                ErrorCode.INVALID_PASSWORD,
                "Password must be at least 12 characters long"
            );
        }

        // Check maximum length (prevent DoS)
        if (password.length() > 128) {
            throw new BusinessException(
                ErrorCode.INVALID_PASSWORD,
                "Password must not exceed 128 characters"
            );
        }

        // Check pattern (uppercase, lowercase, digit, special char)
        if (!STRONG_PASSWORD.matcher(password).matches()) {
            throw new BusinessException(
                ErrorCode.INVALID_PASSWORD,
                "Password must contain at least one uppercase letter, one lowercase letter, " +
                "one digit, and one special character (@$!%*?&#^+=)"
            );
        }

        // Check against common passwords
        String lowerPassword = password.toLowerCase();
        for (String common : COMMON_PASSWORDS) {
            if (lowerPassword.contains(common)) {
                log.warn("User attempted to use common password pattern: {}", common);
                throw new BusinessException(
                    ErrorCode.INVALID_PASSWORD,
                    "Password contains common words or patterns. Please choose a more unique password."
                );
            }
        }

        // Check for repeated characters (e.g., "aaaaaa")
        if (hasRepeatedCharacters(password, 4)) {
            throw new BusinessException(
                ErrorCode.INVALID_PASSWORD,
                "Password cannot contain more than 3 consecutive identical characters"
            );
        }

        // Check for sequential characters (e.g., "12345", "abcde")
        if (hasSequentialCharacters(password, 4)) {
            throw new BusinessException(
                ErrorCode.INVALID_PASSWORD,
                "Password cannot contain more than 3 consecutive sequential characters"
            );
        }

        log.debug("Password validation passed");
    }

    /**
     * Check if password contains repeated characters
     */
    private boolean hasRepeatedCharacters(String password, int maxRepeat) {
        char[] chars = password.toCharArray();
        int repeatCount = 1;
        char lastChar = chars[0];

        for (int i = 1; i < chars.length; i++) {
            if (chars[i] == lastChar) {
                repeatCount++;
                if (repeatCount >= maxRepeat) {
                    return true;
                }
            } else {
                repeatCount = 1;
                lastChar = chars[i];
            }
        }
        return false;
    }

    /**
     * Check if password contains sequential characters
     */
    private boolean hasSequentialCharacters(String password, int maxSeq) {
        char[] chars = password.toLowerCase().toCharArray();
        
        for (int i = 0; i <= chars.length - maxSeq; i++) {
            boolean isSequential = true;
            for (int j = 1; j < maxSeq; j++) {
                if (chars[i + j] != chars[i + j - 1] + 1) {
                    isSequential = false;
                    break;
                }
            }
            if (isSequential) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate password strength (returns boolean instead of throwing)
     * Useful for client-side validation feedback
     */
    public boolean isValid(String password) {
        try {
            validate(password);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    /**
     * Get password strength score (0-100)
     */
    public int getStrengthScore(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        // Length score (max 30 points)
        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 10;
        if (password.length() >= 20) score += 10;

        // Character variety (max 40 points)
        if (password.matches(".*[a-z].*")) score += 10; // lowercase
        if (password.matches(".*[A-Z].*")) score += 10; // uppercase
        if (password.matches(".*\\d.*")) score += 10;   // digit
        if (password.matches(".*[@$!%*?&#^+=].*")) score += 10; // special

        // Complexity bonus (max 30 points)
        if (password.length() >= 16 && hasAllCharacterTypes(password)) score += 15;
        if (!hasRepeatedCharacters(password, 3)) score += 10;
        if (!hasSequentialCharacters(password, 3)) score += 5;

        return Math.min(score, 100);
    }

    private boolean hasAllCharacterTypes(String password) {
        return password.matches(".*[a-z].*") &&
               password.matches(".*[A-Z].*") &&
               password.matches(".*\\d.*") &&
               password.matches(".*[@$!%*?&#^+=].*");
    }
}

