package com.gaminggear.repository;

import com.gaminggear.model.Category;
import com.gaminggear.storage.InMemoryDataStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryRepository {

    private final InMemoryDataStore dataStore;

    public CategoryRepository(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<Category> findAll() {
        return new ArrayList<>(dataStore.getCategories().values());
    }

    public Optional<Category> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(dataStore.getCategories().get(id));
    }

    public Optional<Category> findByNameIgnoreCase(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String normalized = name.trim();
        return dataStore.getCategories().values().stream()
                .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(normalized))
                .findFirst();
    }

    public Category save(Category category) {
        dataStore.getCategories().put(category.getId(), category);
        return category;
    }

    public boolean deleteById(String id) {
        return dataStore.getCategories().remove(id) != null;
    }
}
