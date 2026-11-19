package com.gaminggear.repository;

import com.gaminggear.model.Order;
import com.gaminggear.storage.InMemoryDataStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderRepository {

    private final InMemoryDataStore dataStore;

    public OrderRepository(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<Order> findAll() {
        return new ArrayList<>(dataStore.getOrders().values());
    }

    public Optional<Order> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(dataStore.getOrders().get(id));
    }

    public List<Order> findByCustomerId(String customerId) {
        List<Order> result = new ArrayList<>();
        for (Order order : dataStore.getOrders().values()) {
            if (customerId != null && customerId.equals(order.getCustomerId())) {
                result.add(order);
            }
        }
        return result;
    }

    public Order save(Order order) {
        dataStore.getOrders().put(order.getId(), order);
        return order;
    }

    public boolean deleteById(String id) {
        return dataStore.getOrders().remove(id) != null;
    }
}
