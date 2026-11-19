package com.gaminggear.storage;

import com.gaminggear.config.AppConfig;

/**
 * Coordinates automatic JSON persistence after mutations when enabled in AppConfig.
 */
public class StorageSync {

    private final AppConfig appConfig;
    private final InMemoryDataStore dataStore;
    private final JSONStorageService jsonStorageService;

    public StorageSync(AppConfig appConfig, InMemoryDataStore dataStore, JSONStorageService jsonStorageService) {
        this.appConfig = appConfig;
        this.dataStore = dataStore;
        this.jsonStorageService = jsonStorageService;
    }

    public void persistIfEnabled() {
        if (appConfig != null && appConfig.isAutoSaveEnabled() && jsonStorageService != null) {
            jsonStorageService.save(dataStore);
        }
    }

    public void forceSave() {
        if (jsonStorageService != null) {
            jsonStorageService.save(dataStore);
        }
    }
}
