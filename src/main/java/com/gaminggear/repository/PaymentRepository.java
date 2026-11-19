package com.gaminggear.repository;

import com.gaminggear.model.Payment;
import com.gaminggear.storage.InMemoryDataStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaymentRepository {

    private final InMemoryDataStore dataStore;

    public PaymentRepository(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<Payment> findAll() {
        return new ArrayList<>(dataStore.getPayments().values());
    }

    public Optional<Payment> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(dataStore.getPayments().get(id));
    }

    public List<Payment> findByOrderId(String orderId) {
        List<Payment> result = new ArrayList<>();
        for (Payment payment : dataStore.getPayments().values()) {
            if (orderId != null && orderId.equals(payment.getOrderId())) {
                result.add(payment);
            }
        }
        return result;
    }

    public Payment save(Payment payment) {
        dataStore.getPayments().put(payment.getId(), payment);
        return payment;
    }
}
