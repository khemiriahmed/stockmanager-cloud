package com.stockmanager.storage;

public interface FileStorageService {

    String store(String key, byte[] content, String contentType);

    StoredDocument load(String key, String filename);
}
