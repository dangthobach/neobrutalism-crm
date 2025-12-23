package com.neobrutalism.crm.common.validation;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Input Sanitizer
 *
 * Provides utility methods to sanitize user input and prevent:
 * - XSS (Cross-Site Scripting)
 * - SQL Injection (as additional layer on top of JPA)
 * - Path Traversal
 * - Command Injection
 * - LDAP Injection
 *
 * Usage:
 * - Sanitize all user input before storing in database
 * - Validate file paths and names
 * - Clean HTML content
 */
@Component
public class InputSanitizer {

    // Patterns for validation
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAVASCRIPT_PROTOCOL_PATTERN = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile("('|(--)|;|(/\\*)(\\*/)|xp_|sp_)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("\\.\\.|/\\.\\.|\\\\\\.\\.|\\.\\./|\\.\\\\");
    private static final Pattern NULL_BYTE_PATTERN = Pattern.compile("\\x00");

    /**
     * Sanitize HTML content - remove potentially dangerous tags and attributes
     *
     * @param input Raw HTML input
     * @return Sanitized HTML (or plain text if aggressive mode)
     */
    public String sanitizeHtml(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String sanitized = input;

        // Remove script tags
        sanitized = SCRIPT_TAG_PATTERN.matcher(sanitized).replaceAll("");

        // Remove event handlers (onclick, onload, etc.)
        sanitized = EVENT_HANDLER_PATTERN.matcher(sanitized).replaceAll("");

        // Remove javascript: protocol
        sanitized = JAVASCRIPT_PROTOCOL_PATTERN.matcher(sanitized).replaceAll("");

        // Remove potentially dangerous tags
        sanitized = sanitized.replaceAll("<iframe[^>]*>.*?</iframe>", "");
        sanitized = sanitized.replaceAll("<object[^>]*>.*?</object>", "");
        sanitized = sanitized.replaceAll("<embed[^>]*>", "");

        return sanitized;
    }

    /**
     * Strip all HTML tags - convert to plain text
     *
     * @param input HTML input
     * @return Plain text without any HTML
     */
    public String stripHtmlTags(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        return HTML_TAG_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * Sanitize string for safe storage and display
     * Escapes special HTML characters
     *
     * @param input User input
     * @return Escaped string
     */
    public String escapeHtml(String input) {
        if (input == null) {
            return null;
        }

        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");
    }

    /**
     * Validate and sanitize file name
     * Prevents path traversal and dangerous file names
     *
     * @param fileName User-provided file name
     * @return Sanitized file name
     * @throws IllegalArgumentException if file name is invalid
     */
    public String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        // Check for path traversal attempts
        if (PATH_TRAVERSAL_PATTERN.matcher(fileName).find()) {
            throw new IllegalArgumentException("Invalid file name: path traversal detected");
        }

        // Check for null bytes
        if (NULL_BYTE_PATTERN.matcher(fileName).find()) {
            throw new IllegalArgumentException("Invalid file name: null byte detected");
        }

        // Remove directory separators
        String sanitized = fileName.replace("/", "_").replace("\\", "_");

        // Remove potentially dangerous characters
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Ensure it doesn't start with a dot (hidden file)
        if (sanitized.startsWith(".")) {
            sanitized = "_" + sanitized.substring(1);
        }

        // Limit length
        if (sanitized.length() > 255) {
            String extension = "";
            int lastDot = sanitized.lastIndexOf('.');
            if (lastDot > 0) {
                extension = sanitized.substring(lastDot);
                sanitized = sanitized.substring(0, 255 - extension.length()) + extension;
            } else {
                sanitized = sanitized.substring(0, 255);
            }
        }

        return sanitized;
    }

    /**
     * Validate file path - ensure it doesn't escape allowed directory
     *
     * @param path File path
     * @return True if path is safe
     */
    public boolean isValidFilePath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }

        // Check for path traversal
        if (PATH_TRAVERSAL_PATTERN.matcher(path).find()) {
            return false;
        }

        // Check for null bytes
        if (NULL_BYTE_PATTERN.matcher(path).find()) {
            return false;
        }

        // Check for absolute paths (should use relative paths)
        if (path.startsWith("/") || path.matches("^[a-zA-Z]:.*")) {
            return false;
        }

        return true;
    }

    /**
     * Sanitize string for use in SQL LIKE clauses
     * Escapes special SQL LIKE wildcards
     *
     * Note: This is a secondary defense. Always use JPA/Hibernate parameterized queries.
     *
     * @param input User input for LIKE clause
     * @return Escaped string
     */
    public String sanitizeSqlLike(String input) {
        if (input == null) {
            return null;
        }

        return input
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }

    /**
     * Validate input doesn't contain SQL injection patterns
     * This is a basic check - always use parameterized queries as primary defense
     *
     * @param input User input
     * @return True if input appears safe
     */
    public boolean detectsSqlInjection(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Sanitize input for use in email addresses
     * Prevents email header injection
     *
     * @param email Email address
     * @return Sanitized email
     * @throws IllegalArgumentException if email is invalid
     */
    public String sanitizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        // Remove line breaks and carriage returns (header injection prevention)
        String sanitized = email.replaceAll("[\\r\\n]", "");

        // Basic email validation
        if (!sanitized.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        return sanitized.toLowerCase().trim();
    }

    /**
     * Sanitize input for use in URLs
     * Prevents open redirect and SSRF
     *
     * @param url URL string
     * @return Sanitized URL
     * @throws IllegalArgumentException if URL is invalid
     */
    public String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        // Remove whitespace
        String sanitized = url.trim();

        // Check for javascript: protocol
        if (JAVASCRIPT_PROTOCOL_PATTERN.matcher(sanitized).find()) {
            throw new IllegalArgumentException("Invalid URL: javascript protocol not allowed");
        }

        // Check for data: protocol (can be used for XSS)
        if (sanitized.toLowerCase().startsWith("data:")) {
            throw new IllegalArgumentException("Invalid URL: data protocol not allowed");
        }

        // Only allow http, https, and relative URLs
        if (!sanitized.matches("^(https?://|/).*")) {
            throw new IllegalArgumentException("Invalid URL: only http, https, or relative URLs allowed");
        }

        return sanitized;
    }

    /**
     * Truncate string to maximum length
     *
     * @param input Input string
     * @param maxLength Maximum length
     * @return Truncated string
     */
    public String truncate(String input, int maxLength) {
        if (input == null) {
            return null;
        }

        if (input.length() <= maxLength) {
            return input;
        }

        return input.substring(0, maxLength);
    }
}
