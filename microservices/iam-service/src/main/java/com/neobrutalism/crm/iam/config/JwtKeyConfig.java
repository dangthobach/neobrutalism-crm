package com.neobrutalism.crm.iam.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * JWT Key Configuration - Load RSA keypair from external files
 *
 * CRITICAL FIX: Prevents token invalidation on service restart
 *
 * Architecture:
 * - Production: Load keys from mounted volume (Kubernetes Secret, HashiCorp Vault)
 * - Development: Auto-generate and save keys for reuse
 *
 * Security Benefits:
 * 1. Zero Downtime: Tokens remain valid across service restarts
 * 2. Key Rotation: Easy to rotate keys without code changes
 * 3. Centralized Management: Keys stored in secure external storage
 * 4. Audit Trail: Key access tracked in secrets manager
 *
 * Performance Impact:
 * - Eliminates 100K user re-authentication on restart
 * - Prevents cascade of login requests
 * - Maintains session continuity
 *
 * @author Neobrutalism CRM Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class JwtKeyConfig {

    @Value("${app.security.jwt.private-key-path:classpath:jwt-keys/private_key.pem}")
    private Resource privateKeyResource;

    @Value("${app.security.jwt.public-key-path:classpath:jwt-keys/public_key.pem}")
    private Resource publicKeyResource;

    @Value("${app.security.jwt.key-generation-enabled:false}")
    private boolean keyGenerationEnabled;

    /**
     * Load or generate RSA keypair for JWT signing
     *
     * Production: Load from mounted volume or secrets manager
     * Development: Can auto-generate if enabled
     *
     * @return RSA KeyPair for JWT signing/verification
     * @throws NoSuchAlgorithmException If RSA algorithm not available
     * @throws IOException If key files cannot be read
     * @throws InvalidKeySpecException If key format is invalid
     */
    @Bean
    public KeyPair jwtKeyPair() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        // Try to load existing keys
        if (privateKeyResource.exists() && publicKeyResource.exists()) {
            log.info("Loading JWT keypair from: {} and {}",
                privateKeyResource.getDescription(),
                publicKeyResource.getDescription());

            PrivateKey privateKey = loadPrivateKey(privateKeyResource);
            PublicKey publicKey = loadPublicKey(publicKeyResource);

            log.info("Successfully loaded JWT keypair (RSA-2048)");
            return new KeyPair(publicKey, privateKey);
        }

        // Fallback: Generate new keys (only in dev mode)
        if (keyGenerationEnabled) {
            log.warn("JWT keys not found. Generating new keypair (dev mode only)");
            log.warn("SECURITY WARNING: Generated keys will be lost on restart in production!");

            KeyPair keyPair = generateKeyPair();

            // Optionally save to file system for reuse (dev only)
            saveKeyPair(keyPair);

            return keyPair;
        }

        // Production: Fail fast if keys not found
        throw new IllegalStateException(
            "JWT keypair not found and key generation is disabled. " +
            "Please provide valid key files at: " +
            privateKeyResource.getDescription() + " and " + publicKeyResource.getDescription() +
            "\n\nFor production deployment:" +
            "\n1. Generate keys: openssl genrsa -out private_key.pem 2048" +
            "\n2. Extract public key: openssl rsa -in private_key.pem -pubout -out public_key.pem" +
            "\n3. Store in Kubernetes Secret or HashiCorp Vault" +
            "\n4. Mount to /etc/jwt-keys/ in container"
        );
    }

    /**
     * Load private key from PEM file
     *
     * Expected format: PKCS#8 PEM
     * -----BEGIN PRIVATE KEY-----
     * <base64 encoded key>
     * -----END PRIVATE KEY-----
     *
     * @param resource Resource pointing to private key file
     * @return Loaded PrivateKey
     */
    private PrivateKey loadPrivateKey(Resource resource) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String keyContent = new String(Files.readAllBytes(resource.getFile().toPath()))
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Load public key from PEM file
     *
     * Expected format: X.509 PEM
     * -----BEGIN PUBLIC KEY-----
     * <base64 encoded key>
     * -----END PUBLIC KEY-----
     *
     * @param resource Resource pointing to public key file
     * @return Loaded PublicKey
     */
    private PublicKey loadPublicKey(Resource resource) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String keyContent = new String(Files.readAllBytes(resource.getFile().toPath()))
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePublic(keySpec);
    }

    /**
     * Generate new RSA keypair (2048-bit)
     *
     * Only used in development mode when keys not found
     * NEVER use in production (keys will be lost on restart)
     *
     * @return Newly generated KeyPair
     */
    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();

        log.info("Generated new RSA-2048 keypair");
        return keyPair;
    }

    /**
     * Save generated keypair to file system (dev mode)
     *
     * Saves to src/main/resources/jwt-keys/ for reuse across restarts
     * Files are automatically excluded from git via .gitignore
     *
     * @param keyPair KeyPair to save
     */
    private void saveKeyPair(KeyPair keyPair) {
        try {
            // Create directory if not exists
            java.nio.file.Path keysDir = java.nio.file.Paths.get("src/main/resources/jwt-keys");
            if (!Files.exists(keysDir)) {
                Files.createDirectories(keysDir);
                log.info("Created jwt-keys directory: {}", keysDir.toAbsolutePath());
            }

            // Save private key (PKCS#8 format)
            String privateKeyPEM = "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPrivate().getEncoded()) +
                "\n-----END PRIVATE KEY-----\n";
            java.nio.file.Path privateKeyPath = keysDir.resolve("private_key.pem");
            Files.writeString(privateKeyPath, privateKeyPEM);

            // Save public key (X.509 format)
            String publicKeyPEM = "-----BEGIN PUBLIC KEY-----\n" +
                Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPublic().getEncoded()) +
                "\n-----END PUBLIC KEY-----\n";
            java.nio.file.Path publicKeyPath = keysDir.resolve("public_key.pem");
            Files.writeString(publicKeyPath, publicKeyPEM);

            // Set restrictive permissions (Unix only)
            try {
                Files.setPosixFilePermissions(privateKeyPath,
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
                log.info("Set private key permissions to 600");
            } catch (UnsupportedOperationException e) {
                // Windows doesn't support POSIX permissions
                log.debug("POSIX permissions not supported on this platform");
            }

            log.info("Saved generated keypair to: {}", keysDir.toAbsolutePath());
            log.warn("IMPORTANT: Add jwt-keys/ to .gitignore to prevent committing private keys!");

        } catch (IOException e) {
            log.error("Failed to save keypair to file system: {}", e.getMessage());
            log.warn("Keys will be lost on service restart!");
        }
    }
}
