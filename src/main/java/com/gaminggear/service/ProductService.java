package com.gaminggear.service;

import com.gaminggear.exception.BusinessException;
import com.gaminggear.exception.ResourceNotFoundException;
import com.gaminggear.model.Product;
import com.gaminggear.model.enums.ProductStatus;
import com.gaminggear.repository.CategoryRepository;
import com.gaminggear.repository.ProductRepository;
import com.gaminggear.storage.StorageSync;
import com.gaminggear.util.CurrencyUtil;
import com.gaminggear.util.IdGenerator;
import com.gaminggear.util.ValidationUtil;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProductService {

    public enum SortOption {
        NAME_ASC("Tên A -> Z"),
        NAME_DESC("Tên Z -> A"),
        PRICE_ASC("Giá tăng dần"),
        PRICE_DESC("Giá giảm dần"),
        STOCK_ASC("Tồn kho tăng dần");

        private final String label;

        SortOption(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StorageSync storageSync;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          StorageSync storageSync) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.storageSync = storageSync;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + id));
    }

    public Product createProduct(String name, String categoryId, String brand, BigDecimal price,
                                 int stockQuantity, String description, String imagePath,
                                 ProductStatus status) {
        String cleanName = ValidationUtil.requireNonBlank(name, "Tên sản phẩm");
        String cleanBrand = ValidationUtil.requireNonBlank(brand, "Thương hiệu");
        ValidationUtil.requireNonBlank(categoryId, "Danh mục");
        if (categoryRepository.findById(categoryId).isEmpty()) {
            throw new ResourceNotFoundException("Danh mục không tồn tại: " + categoryId);
        }
        BigDecimal validPrice = CurrencyUtil.normalize(ValidationUtil.requirePositive(price, "Giá bán"));
        int validStock = ValidationUtil.requireNonNegativeInt(stockQuantity, "Số lượng tồn kho");

        ProductStatus resolvedStatus = resolveStatus(status, validStock);
        Product product = new Product(
                IdGenerator.nextId("PRD"),
                cleanName,
                categoryId,
                cleanBrand,
                validPrice,
                validStock,
                description == null ? "" : description.trim(),
                imagePath == null || imagePath.isBlank() ? "assets/products/default.png" : imagePath.trim(),
                resolvedStatus
        );
        productRepository.save(product);
        persist();
        return product;
    }

    public Product updateProduct(String id, String name, String categoryId, String brand,
                                 BigDecimal price, int stockQuantity, String description,
                                 String imagePath, ProductStatus status) {
        Product existing = getProductById(id);
        String cleanName = ValidationUtil.requireNonBlank(name, "Tên sản phẩm");
        String cleanBrand = ValidationUtil.requireNonBlank(brand, "Thương hiệu");
        ValidationUtil.requireNonBlank(categoryId, "Danh mục");
        if (categoryRepository.findById(categoryId).isEmpty()) {
            throw new ResourceNotFoundException("Danh mục không tồn tại: " + categoryId);
        }
        BigDecimal validPrice = CurrencyUtil.normalize(ValidationUtil.requirePositive(price, "Giá bán"));
        int validStock = ValidationUtil.requireNonNegativeInt(stockQuantity, "Số lượng tồn kho");

        existing.setName(cleanName);
        existing.setCategoryId(categoryId);
        existing.setBrand(cleanBrand);
        existing.setPrice(validPrice);
        existing.setStockQuantity(validStock);
        existing.setDescription(description == null ? "" : description.trim());
        existing.setImagePath(imagePath == null || imagePath.isBlank() ? existing.getImagePath() : imagePath.trim());
        existing.setStatus(resolveStatus(status, validStock));

        productRepository.save(existing);
        persist();
        return existing;
    }

    public void deleteProduct(String id) {
        getProductById(id);
        productRepository.deleteById(id);
        persist();
    }

    public int deleteProducts(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất một sản phẩm để xóa.");
        }
        int deleted = productRepository.deleteAllByIds(ids);
        persist();
        return deleted;
    }

    public List<String> getAllBrands() {
        return productRepository.findAll().stream()
                .map(Product::getBrand)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
    }

    public List<Product> searchAndFilter(String keyword, String categoryId, String brand,
                                         BigDecimal minPrice, BigDecimal maxPrice,
                                         SortOption sortOption) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Giá tối thiểu không được âm.");
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Giá tối đa không được âm.");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException("Giá tối thiểu không được lớn hơn giá tối đa.");
        }

        Comparator<Product> comparator = switch (sortOption == null ? SortOption.NAME_ASC : sortOption) {
            case NAME_DESC -> Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER).reversed();
            case PRICE_ASC -> Comparator.comparing(Product::getPrice);
            case PRICE_DESC -> Comparator.comparing(Product::getPrice).reversed();
            case STOCK_ASC -> Comparator.comparingInt(Product::getStockQuantity);
            case NAME_ASC -> Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
        };

        return productRepository.findAll().stream()
                .filter(p -> normalizedKeyword.isEmpty()
                        || (p.getName() != null && p.getName().toLowerCase().contains(normalizedKeyword))
                        || (p.getBrand() != null && p.getBrand().toLowerCase().contains(normalizedKeyword))
                        || (p.getId() != null && p.getId().toLowerCase().contains(normalizedKeyword)))
                .filter(p -> categoryId == null || categoryId.isBlank() || categoryId.equals(p.getCategoryId()))
                .filter(p -> brand == null || brand.isBlank() || brand.equalsIgnoreCase(p.getBrand()))
                .filter(p -> minPrice == null || p.getPrice().compareTo(minPrice) >= 0)
                .filter(p -> maxPrice == null || p.getPrice().compareTo(maxPrice) <= 0)
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private ProductStatus resolveStatus(ProductStatus requested, int stockQuantity) {
        if (requested == ProductStatus.INACTIVE) {
            return ProductStatus.INACTIVE;
        }
        return stockQuantity <= 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE;
    }

    private void persist() {
        if (storageSync != null) {
            storageSync.persistIfEnabled();
        }
    }
}
