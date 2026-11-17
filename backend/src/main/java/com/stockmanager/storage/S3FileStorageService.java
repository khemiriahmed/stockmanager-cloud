package com.stockmanager.storage;

import com.stockmanager.common.exception.BusinessException;
import com.stockmanager.config.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3FileStorageService.class);

    private final S3Client s3Client;
    private final String bucket;

    public S3FileStorageService(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.bucket = properties.s3Bucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3_BUCKET_DOCUMENTS doit être renseigné lorsque STORAGE_TYPE=s3.");
        }
    }

    @Override
    public String store(String key, byte[] content, String contentType) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(content));
            log.info("Document envoyé sur S3: s3://{}/{}", bucket, key);
            return key;
        } catch (Exception ex) {
            log.error("Échec de l'envoi S3 du document {}", key, ex);
            throw new BusinessException("Impossible d'enregistrer le document dans S3.", "STORAGE_ERROR",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public StoredDocument load(String key, String filename) {
        try {
            byte[] bytes = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
            return new StoredDocument(key, bytes, "application/pdf", filename);
        } catch (Exception ex) {
            log.error("Échec de la lecture S3 du document {}", key, ex);
            throw BusinessException.notFound("Document introuvable dans le stockage.");
        }
    }
}
