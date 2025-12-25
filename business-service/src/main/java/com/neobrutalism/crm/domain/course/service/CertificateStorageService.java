package com.neobrutalism.crm.domain.course.service;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Service for storing and retrieving certificate PDFs
 * Uses MinIO via FileStorageService for object storage
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateStorageService {

    private final MinioClient minioClient;
    
    @Value("${minio.bucket.certificates:certificates}")
    private String certificatesBucket;
    
    @Value("${app.base-url:https://your-domain.com}")
    private String applicationBaseUrl;
    
    private static final String CERTIFICATES_FOLDER = "pdfs/";

    /**
     * Store certificate PDF in MinIO
     * 
     * @param certificateNumber Certificate number
     * @param pdfContent PDF content as byte array
     * @return Public URL to access the PDF
     * @throws IOException if storage fails
     */
    public String storeCertificatePdf(String certificateNumber, byte[] pdfContent) throws IOException {
        log.info("Storing PDF for certificate: {}", certificateNumber);

        try {
            String fileName = CERTIFICATES_FOLDER + certificateNumber + ".pdf";
            
            // Ensure bucket exists
            ensureBucketExists();
            
            // Upload PDF
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(certificatesBucket)
                    .object(fileName)
                    .stream(new ByteArrayInputStream(pdfContent), pdfContent.length, -1)
                    .contentType("application/pdf")
                    .build()
            );

            // Generate public URL
            String pdfUrl = applicationBaseUrl + "/api/certificates/" + certificateNumber + "/pdf";
            
            log.info("Certificate PDF stored successfully: {}", pdfUrl);
            return pdfUrl;

        } catch (Exception e) {
            log.error("Failed to store certificate PDF for {}: {}", certificateNumber, e.getMessage(), e);
            throw new IOException("Failed to store certificate PDF", e);
        }
    }

    /**
     * Delete certificate PDF from MinIO
     * 
     * @param certificateNumber Certificate number
     * @throws IOException if deletion fails
     */
    public void deleteCertificatePdf(String certificateNumber) throws IOException {
        log.info("Deleting PDF for certificate: {}", certificateNumber);

        try {
            String fileName = CERTIFICATES_FOLDER + certificateNumber + ".pdf";
            
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(certificatesBucket)
                    .object(fileName)
                    .build()
            );
            
            log.info("Certificate PDF deleted successfully: {}", certificateNumber);

        } catch (Exception e) {
            log.error("Failed to delete certificate PDF for {}: {}", certificateNumber, e.getMessage(), e);
            throw new IOException("Failed to delete certificate PDF", e);
        }
    }

    /**
     * Check if certificate PDF exists in MinIO
     * 
     * @param certificateNumber Certificate number
     * @return true if PDF exists, false otherwise
     */
    public boolean certificatePdfExists(String certificateNumber) {
        try {
            String fileName = CERTIFICATES_FOLDER + certificateNumber + ".pdf";
            
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(certificatesBucket)
                    .object(fileName)
                    .build()
            );
            return true;
        } catch (Exception e) {
            log.debug("Certificate PDF does not exist for {}: {}", certificateNumber, e.getMessage());
            return false;
        }
    }

    /**
     * Get certificate PDF URL
     * 
     * @param certificateNumber Certificate number
     * @return Public URL to access the PDF, or null if not found
     */
    public String getCertificatePdfUrl(String certificateNumber) {
        if (certificatePdfExists(certificateNumber)) {
            return applicationBaseUrl + "/api/certificates/" + certificateNumber + "/pdf";
        }
        return null;
    }
    
    /**
     * Ensure certificates bucket exists
     */
    private void ensureBucketExists() throws Exception {
        boolean found = minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(certificatesBucket)
                .build()
        );
        
        if (!found) {
            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(certificatesBucket)
                    .build()
            );
            log.info("Created certificates bucket: {}", certificatesBucket);
        }
    }
}
