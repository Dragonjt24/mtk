package com.gaminggear.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gaminggear.model.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {
    private String id;
    private String name;
    private String categoryId;
    private String brand;
    private BigDecimal price;
    private int stockQuantity;
    private String description;
    private String imagePath;
    private ProductStatus status;

    public Product() {
    }

    public Product(String id, String name, String categoryId, String brand,
                   BigDecimal price, int stockQuantity, String description,
                   String imagePath, ProductStatus status) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
        this.brand = brand;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.description = description;
        this.imagePath = imagePath;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return name + " (" + brand + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product product)) return false;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
