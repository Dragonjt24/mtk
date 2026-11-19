package com.gaminggear.repository;

import com.gaminggear.model.Customer;
import com.gaminggear.storage.InMemoryDataStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerRepository {

    private final InMemoryDataStore dataStore;

    public CustomerRepository(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<Customer> findAll() {
        return new ArrayList<>(dataStore.getCustomers().values());
    }

    public Optional<Customer> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(dataStore.getCustomers().get(id));
    }

    public Optional<Customer> findByPhone(String phone) {
        if (phone == null) {
            return Optional.empty();
        }
        return dataStore.getCustomers().values().stream()
                .filter(c -> phone.equals(c.getPhone()))
                .findFirst();
    }

    public Customer save(Customer customer) {
        dataStore.getCustomers().put(customer.getId(), customer);
        return customer;
    }

    public boolean deleteById(String id) {
        return dataStore.getCustomers().remove(id) != null;
    }
}
