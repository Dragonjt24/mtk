package com.gaminggear.repository;

import com.gaminggear.model.InventoryLog;
import com.gaminggear.storage.InMemoryDataStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InventoryLogRepository {

    private final InMemoryDataStore dataStore;

    public InventoryLogRepository(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<InventoryLog> findAll() {
        List<InventoryLog> copy = new ArrayList<>(dataStore.getInventoryLogs());
        Collections.reverse(copy);
        return copy;
    }

    public InventoryLog save(InventoryLog log) {
        dataStore.getInventoryLogs().add(log);
        return log;
    }
}
