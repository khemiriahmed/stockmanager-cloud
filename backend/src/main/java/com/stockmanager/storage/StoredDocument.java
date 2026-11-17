package com.stockmanager.storage;

public record StoredDocument(String key, byte[] content, String contentType, String filename) {
}
