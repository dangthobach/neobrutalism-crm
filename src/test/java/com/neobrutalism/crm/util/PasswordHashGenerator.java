package com.neobrutalism.crm.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes
 * Run this as a Java application to generate hashes
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

        // Generate hash for "admin123"
        String password = "admin123";
        String hash = encoder.encode(password);

        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash (strength 10): " + hash);
        System.out.println();
        System.out.println("SQL UPDATE:");
        System.out.println("UPDATE users SET password_hash = '" + hash + "' WHERE username = 'admin';");
    }
}
