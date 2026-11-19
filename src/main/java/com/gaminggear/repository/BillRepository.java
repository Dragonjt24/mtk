package com.gaminggear.repository;

import com.gaminggear.model.Bill;
import com.gaminggear.storage.InMemoryDataStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BillRepository {

    private final InMemoryDataStore dataStore;

    public BillRepository(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<Bill> findAll() {
        return new ArrayList<>(dataStore.getBills().values());
    }

    public Optional<Bill> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(dataStore.getBills().get(id));
    }

    public Optional<Bill> findByOrderId(String orderId) {
        if (orderId == null) {
            return Optional.empty();
        }
        return dataStore.getBills().values().stream()
                .filter(b -> orderId.equals(b.getOrderId()))
                .findFirst();
    }

    public Bill save(Bill bill) {
        dataStore.getBills().put(bill.getId(), bill);
        return bill;
    }
}
