package com.gaminggear.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gaminggear.config.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Isolated file persistence helper using Jackson JSON.
 * Handles missing or corrupted JSON files gracefully without treating JSON as a database.
 */
public class JSONStorageService {

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;

    public JSONStorageService(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public synchronized void save(InMemoryDataStore dataStore) {
        if (dataStore == null) {
            return;
        }
        Path path = appConfig.getStoragePath();
        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            DataSnapshot snapshot = dataStore.toSnapshot();
            objectMapper.writeValue(path.toFile(), snapshot);
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể ghi dữ liệu ra tệp JSON: " + path, ex);
        }
    }

    public synchronized boolean load(InMemoryDataStore dataStore) {
        if (dataStore == null) {
            return false;
        }
        Path path = appConfig.getStoragePath();
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return false;
        }
        try {
            if (Files.size(path) == 0) {
                return false;
            }
            DataSnapshot snapshot = objectMapper.readValue(path.toFile(), DataSnapshot.class);
            if (snapshot == null || snapshot.getUsers().isEmpty() || snapshot.getCategories().isEmpty()) {
                return false;
            }
            dataStore.loadFromSnapshot(snapshot);
            return true;
        } catch (IOException ex) {
            // Corrupted or unreadable JSON: fall back safely to sample seed data
            return false;
        }
    }
}
