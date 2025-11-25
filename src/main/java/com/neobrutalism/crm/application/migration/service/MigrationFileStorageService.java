package com.neobrutalism.crm.application.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service for storing and retrieving Excel migration files
 */
@Slf4j
@Service
public class MigrationFileStorageService {
    
    @Value("${app.migration.file-storage.path:./data/migration-files}")
    private String storagePath;
    
    /**
     * Store uploaded file and return file identifier
     * ✅ FIX: Use try-with-resources to ensure InputStream is closed
     */
    public String storeFile(MultipartFile file, UUID jobId) throws IOException {
        Path storageDir = Paths.get(storagePath);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        String fileName = jobId.toString() + "_" + file.getOriginalFilename();
        Path filePath = storageDir.resolve(fileName);

        // ✅ FIX: Explicitly close InputStream after copy
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Stored migration file: {} for job: {}", fileName, jobId);
        return fileName;
    }
    
    /**
     * Retrieve file input stream by job ID
     * Returns BufferedInputStream to support mark/reset operations
     */
    public InputStream retrieveFile(UUID jobId, String originalFileName) throws IOException {
        String fileName = jobId.toString() + "_" + originalFileName;
        Path filePath = Paths.get(storagePath).resolve(fileName);

        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + fileName);
        }

        // ✅ FIX: Wrap with BufferedInputStream to support mark/reset for ExcelEarlyValidator
        return new java.io.BufferedInputStream(Files.newInputStream(filePath));
    }
    
    /**
     * Delete file after migration is complete
     */
    public void deleteFile(UUID jobId, String originalFileName) {
        try {
            String fileName = jobId.toString() + "_" + originalFileName;
            Path filePath = Paths.get(storagePath).resolve(fileName);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted migration file: {}", fileName);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file for job: {}", jobId, e);
        }
    }
}

