package com.neobrutalism.crm.common.storage;

import com.neobrutalism.crm.common.exception.BusinessException;
import com.neobrutalism.crm.common.exception.ErrorCode;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for file storage operations using MinIO
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioClient minioClient;
    private final String defaultMinioBucket;

    /**
     * Upload file to MinIO
     */
    public String uploadFile(MultipartFile file, String bucket, String objectName) {
        try {
            // Ensure bucket exists
            ensureBucketExists(bucket);

            // Upload file
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("File uploaded successfully: {}/{}", bucket, objectName);
            return objectName;

        } catch (Exception e) {
            log.error("Error uploading file to MinIO: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Upload file with auto-generated name
     */
    public String uploadFile(MultipartFile file, String bucket) {
        String objectName = generateObjectName(file.getOriginalFilename());
        return uploadFile(file, bucket, objectName);
    }

    /**
     * Upload file to default bucket
     */
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, defaultMinioBucket);
    }

    /**
     * Download file from MinIO
     */
    public InputStream downloadFile(String bucket, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error downloading file from MinIO: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to download file: " + e.getMessage());
        }
    }

    /**
     * Download file from default bucket
     */
    public InputStream downloadFile(String objectName) {
        return downloadFile(defaultMinioBucket, objectName);
    }

    /**
     * Delete file from MinIO
     */
    public void deleteFile(String bucket, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            log.info("File deleted successfully: {}/{}", bucket, objectName);
        } catch (Exception e) {
            log.error("Error deleting file from MinIO: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to delete file: " + e.getMessage());
        }
    }

    /**
     * Delete file from default bucket
     */
    public void deleteFile(String objectName) {
        deleteFile(defaultMinioBucket, objectName);
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String bucket, String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get file metadata
     */
    public StatObjectResponse getFileMetadata(String bucket, String objectName) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error getting file metadata from MinIO: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to get file metadata: " + e.getMessage());
        }
    }

    /**
     * Generate presigned URL for temporary access
     */
    public String generatePresignedUrl(String bucket, String objectName, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error generating presigned URL: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to generate presigned URL: " + e.getMessage());
        }
    }

    /**
     * Generate presigned URL with default expiry (15 minutes)
     */
    public String generatePresignedUrl(String bucket, String objectName) {
        return generatePresignedUrl(bucket, objectName, 15);
    }

    /**
     * List files in bucket with prefix
     */
    public List<String> listFiles(String bucket, String prefix) {
        List<String> files = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucket)
                            .prefix(prefix)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                files.add(item.objectName());
            }

            return files;
        } catch (Exception e) {
            log.error("Error listing files from MinIO: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to list files: " + e.getMessage());
        }
    }

    /**
     * Copy file within MinIO
     */
    public void copyFile(String sourceBucket, String sourceObject, String destBucket, String destObject) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(destBucket)
                            .object(destObject)
                            .source(CopySource.builder()
                                    .bucket(sourceBucket)
                                    .object(sourceObject)
                                    .build())
                            .build()
            );
            log.info("File copied successfully: {}/{} -> {}/{}", sourceBucket, sourceObject, destBucket, destObject);
        } catch (Exception e) {
            log.error("Error copying file in MinIO: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to copy file: " + e.getMessage());
        }
    }

    /**
     * Ensure bucket exists, create if not
     */
    private void ensureBucketExists(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucket)
                            .build()
            );

            if (!exists) {
                log.info("Creating bucket: {}", bucket);
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucket)
                                .build()
                );
            }
        } catch (Exception e) {
            log.error("Error ensuring bucket exists: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to ensure bucket exists: " + e.getMessage());
        }
    }

    /**
     * Generate unique object name
     */
    private String generateObjectName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Get file size
     */
    public long getFileSize(String bucket, String objectName) {
        try {
            StatObjectResponse stat = getFileMetadata(bucket, objectName);
            return stat.size();
        } catch (Exception e) {
            log.error("Error getting file size: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Get content type
     */
    public String getContentType(String bucket, String objectName) {
        try {
            StatObjectResponse stat = getFileMetadata(bucket, objectName);
            return stat.contentType();
        } catch (Exception e) {
            log.error("Error getting content type: {}", e.getMessage(), e);
            return "application/octet-stream";
        }
    }
}
