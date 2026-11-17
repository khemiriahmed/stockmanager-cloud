package com.stockmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String type,
        String localPath,
        String s3Bucket,
        String s3Region,
        long presignedUrlMinutes
) {
    public boolean isS3() {
        return "s3".equalsIgnoreCase(type);
    }
}
