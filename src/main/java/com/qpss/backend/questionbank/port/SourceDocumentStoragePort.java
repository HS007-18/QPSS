package com.qpss.backend.questionbank.port;
public interface SourceDocumentStoragePort {
    String storeDocument(byte[] fileBytes, String extension);
    String storeDocument(String tempFilePath, String extension);
    void deleteDocument(String storedFileName);
    byte[] loadDocument(String storedFileName);
}