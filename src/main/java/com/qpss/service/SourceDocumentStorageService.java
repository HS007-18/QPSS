package com.qpss.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class SourceDocumentStorageService {

    private final Path rootLocation;

    public SourceDocumentStorageService(@Value("${qpss.storage.question-banks}") String storageProperty) {
        if (!StringUtils.hasText(storageProperty)) {
            throw new IllegalStateException("Storage property qpss.storage.question-banks is missing");
        }
        this.rootLocation = Paths.get(storageProperty).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory at " + rootLocation, e);
        }
    }

    public String storeDocument(byte[] fileBytes, String extension) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("Failed to store empty file.");
        }

        String storedFileName = UUID.randomUUID().toString() + (extension.startsWith(".") ? extension : "." + extension);
        Path destinationFile = getSafePath(storedFileName);

        try {
            Files.write(destinationFile, fileBytes);
            return storedFileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    public void deleteDocument(String storedFileName) {
        Path target = getSafePath(storedFileName);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
        }
    }

    public byte[] loadDocument(String storedFileName) {
        Path target = getSafePath(storedFileName);
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read stored file.", e);
        }
    }

    private Path getSafePath(String filename) {
        Path destinationFile = this.rootLocation.resolve(Paths.get(filename)).normalize().toAbsolutePath();
        if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
            throw new SecurityException("Cannot store file outside current directory.");
        }
        return destinationFile;
    }
}