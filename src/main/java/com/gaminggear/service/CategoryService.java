package com.gaminggear.service;

import com.gaminggear.exception.BusinessException;
import com.gaminggear.exception.ResourceNotFoundException;
import com.gaminggear.model.Category;
import com.gaminggear.repository.CategoryRepository;
import com.gaminggear.repository.ProductRepository;
import com.gaminggear.storage.StorageSync;
import com.gaminggear.util.IdGenerator;
import com.gaminggear.util.ValidationUtil;

import java.util.List;

public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final StorageSync storageSync;

    public CategoryService(CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           StorageSync storageSync) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.storageSync = storageSync;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public List<Category> getActiveCategories() {
        return categoryRepository.findAll().stream()
                .filter(Category::isActive)
                .toList();
    }

    public Category getCategoryById(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục: " + id));
    }

    public Category createCategory(String name, String description, boolean active) {
        String cleanName = ValidationUtil.requireNonBlank(name, "Tên danh mục");
        if (categoryRepository.findByNameIgnoreCase(cleanName).isPresent()) {
            throw new BusinessException("Danh mục đã tồn tại: " + cleanName);
        }
        Category category = new Category(
                IdGenerator.nextId("CAT"),
                cleanName,
                description == null ? "" : description.trim(),
                active
        );
        categoryRepository.save(category);
        persist();
        return category;
    }

    public Category updateCategory(String id, String name, String description, boolean active) {
        Category existing = getCategoryById(id);
        String cleanName = ValidationUtil.requireNonBlank(name, "Tên danh mục");
        categoryRepository.findByNameIgnoreCase(cleanName)
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> {
                    throw new BusinessException("Tên danh mục đã được sử dụng: " + cleanName);
                });
        existing.setName(cleanName);
        existing.setDescription(description == null ? "" : description.trim());
        existing.setActive(active);
        categoryRepository.save(existing);
        persist();
        return existing;
    }

    public void deleteCategory(String id) {
        getCategoryById(id);
        int linkedCount = countProductsInCategory(id);
        if (linkedCount > 0) {
            throw new BusinessException("Không thể xóa danh mục vì đang có " + linkedCount + " sản phẩm thuộc danh mục này.");
        }
        categoryRepository.deleteById(id);
        persist();
    }

    public int countProductsInCategory(String categoryId) {
        return productRepository.findByCategoryId(categoryId).size();
    }

    private void persist() {
        if (storageSync != null) {
            storageSync.persistIfEnabled();
        }
    }
}
