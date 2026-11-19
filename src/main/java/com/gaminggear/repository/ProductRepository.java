package com.gaminggear.repository;

import com.gaminggear.model.Product;
import com.gaminggear.storage.InMemoryDataStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductRepository {

    private final InMemoryDataStore dataStore;

    public ProductRepository(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<Product> findAll() {
        return new ArrayList<>(dataStore.getProducts().values());
    }

    public Optional<Product> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(dataStore.getProducts().get(id));
    }

    public List<Product> findByCategoryId(String categoryId) {
        List<Product> result = new ArrayList<>();
        for (Product product : dataStore.getProducts().values()) {
            if (categoryId != null && categoryId.equals(product.getCategoryId())) {
                result.add(product);
            }
        }
        return result;
    }

    public Product save(Product product) {
        dataStore.getProducts().put(product.getId(), product);
        return product;
    }

    public boolean deleteById(String id) {
        return dataStore.getProducts().remove(id) != null;
    }

    public int deleteAllByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int deleted = 0;
        for (String id : ids) {
            if (dataStore.getProducts().remove(id) != null) {
                deleted++;
            }
        }
        return deleted;
    }
}
