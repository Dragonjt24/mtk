package com.gaminggear.service;

import com.gaminggear.config.AppConfig;
import com.gaminggear.exception.BusinessException;
import com.gaminggear.exception.ResourceNotFoundException;
import com.gaminggear.model.InventoryLog;
import com.gaminggear.model.Order;
import com.gaminggear.model.OrderItem;
import com.gaminggear.model.Product;
import com.gaminggear.model.enums.ProductStatus;
import com.gaminggear.repository.InventoryLogRepository;
import com.gaminggear.repository.ProductRepository;
import com.gaminggear.storage.StorageSync;
import com.gaminggear.util.IdGenerator;
import com.gaminggear.util.ValidationUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final AppConfig appConfig;
    private final StorageSync storageSync;

    public InventoryService(ProductRepository productRepository,
                            InventoryLogRepository inventoryLogRepository,
                            AppConfig appConfig,
                            StorageSync storageSync) {
        this.productRepository = productRepository;
        this.inventoryLogRepository = inventoryLogRepository;
        this.appConfig = appConfig;
        this.storageSync = storageSync;
    }

    public List<Product> getLowStockProducts() {
        int threshold = appConfig.getLowStockThreshold();
        return productRepository.findAll().stream()
                .filter(p -> p.getStatus() != ProductStatus.INACTIVE)
                .filter(p -> p.getStockQuantity() <= threshold)
                .collect(Collectors.toList());
    }

    public List<InventoryLog> getAllLogs() {
        return inventoryLogRepository.findAll();
    }

    public Product adjustStock(String productId, int delta, String reason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + productId));
        String cleanReason = ValidationUtil.requireNonBlank(reason, "Lý do điều chỉnh kho");

        int previous = product.getStockQuantity();
        int updated = previous + delta;
        if (updated < 0) {
            throw new BusinessException("Số lượng tồn kho sau điều chỉnh không được âm (hiện có: " + previous + ").");
        }

        product.setStockQuantity(updated);
        if (product.getStatus() != ProductStatus.INACTIVE) {
            product.setStatus(updated == 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE);
        }
        productRepository.save(product);

        InventoryLog log = new InventoryLog(
                IdGenerator.nextId("INV"),
                product.getId(),
                product.getName(),
                delta,
                previous,
                updated,
                cleanReason,
                LocalDateTime.now()
        );
        inventoryLogRepository.save(log);
        persist();
        return product;
    }

    public Product setStockQuantity(String productId, int newQuantity, String reason) {
        ValidationUtil.requireNonNegativeInt(newQuantity, "Số lượng tồn kho mới");
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + productId));
        int delta = newQuantity - product.getStockQuantity();
        return adjustStock(productId, delta, reason);
    }

    public void validateStockForOrder(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            throw new BusinessException("Đơn hàng không có sản phẩm.");
        }
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + item.getProductId()));
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new BusinessException("Sản phẩm '" + product.getName() + "' không đủ tồn kho (còn "
                        + product.getStockQuantity() + ", yêu cầu " + item.getQuantity() + ").");
            }
        }
    }

    public synchronized void deductStockForOrder(Order order) {
        validateStockForOrder(order);
        for (OrderItem item : order.getItems()) {
            adjustStock(item.getProductId(), -item.getQuantity(), "Xuất kho đơn hàng " + order.getId());
        }
    }

    private void persist() {
        if (storageSync != null) {
            storageSync.persistIfEnabled();
        }
    }
}
