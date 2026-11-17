package com.stockmanager.storage;

import com.stockmanager.common.exception.BusinessException;
import com.stockmanager.config.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path root;

    public LocalFileStorageService(StorageProperties properties) {
        this.root = Paths.get(properties.localPath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de créer le répertoire de stockage " + root, ex);
        }
    }

    @Override
    public String store(String key, byte[] content, String contentType) {
        Path destination = resolve(key);
        try {
            Files.createDirectories(destination.getParent());
            Files.write(destination, content);
            log.info("Document stocké localement: {}", key);
            return key;
        } catch (IOException ex) {
            throw new BusinessException("Impossible d'enregistrer le document.", "STORAGE_ERROR",
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public StoredDocument load(String key, String filename) {
        Path source = resolve(key);
        if (!Files.exists(source)) {
            throw BusinessException.notFound("Document introuvable.");
        }
        try {
            return new StoredDocument(key, Files.readAllBytes(source), "application/pdf", filename);
        } catch (IOException ex) {
            throw new BusinessException("Impossible de lire le document.", "STORAGE_ERROR",
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new BusinessException("Clé de document invalide.", "INVALID_STORAGE_KEY");
        }
        return path;
    }
}
