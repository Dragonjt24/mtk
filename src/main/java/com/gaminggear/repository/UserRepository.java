package com.gaminggear.repository;

import com.gaminggear.model.User;
import com.gaminggear.storage.InMemoryDataStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {

    private final InMemoryDataStore dataStore;

    public UserRepository(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<User> findAll() {
        return new ArrayList<>(dataStore.getUsers().values());
    }

    public Optional<User> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(dataStore.getUsers().get(id));
    }

    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        String normalized = username.trim();
        return dataStore.getUsers().values().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().equalsIgnoreCase(normalized))
                .findFirst();
    }

    public User save(User user) {
        dataStore.getUsers().put(user.getId(), user);
        return user;
    }

    public boolean deleteById(String id) {
        return dataStore.getUsers().remove(id) != null;
    }
}
