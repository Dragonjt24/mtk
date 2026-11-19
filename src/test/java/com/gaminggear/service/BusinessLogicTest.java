package com.gaminggear.service;

import com.gaminggear.config.AppConfig;
import com.gaminggear.config.DependencyContainer;
import com.gaminggear.exception.BusinessException;
import com.gaminggear.model.Order;
import com.gaminggear.model.Product;
import com.gaminggear.model.enums.OrderStatus;
import com.gaminggear.model.enums.PaymentMethod;
import com.gaminggear.model.enums.ProductStatus;
import com.gaminggear.patterns.factory.DiscountType;
import com.gaminggear.storage.InMemoryDataStore;
import com.gaminggear.storage.JSONStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BusinessLogicTest {

    private DependencyContainer container;

    @BeforeEach
    void setUp() {
        AppConfig config = new AppConfig();
        config.setAutoSaveEnabled(false);
        container = new DependencyContainer(config, false);
    }

    @Test
    void createOrder_withEmptyCart_throwsBusinessException() {
        assertThrows(BusinessException.class, () ->
                container.getOrderService().createOrder("CUS-001", List.of(), DiscountType.NO_DISCOUNT));
    }

    @Test
    void createOrder_exceedingStock_throwsBusinessException() {
        Product product = container.getProductService().getProductById("PRD-018"); // stock = 2
        int requested = product.getStockQuantity() + 5;
        assertThrows(BusinessException.class, () ->
                container.getOrderService().createOrder(
                        "CUS-001",
                        List.of(new OrderService.OrderLineRequest(product.getId(), requested)),
                        DiscountType.NO_DISCOUNT
                ));
    }

    @Test
    void createOrder_withZeroOrNegativeQuantity_throwsBusinessException() {
        assertThrows(BusinessException.class, () ->
                container.getOrderService().createOrder(
                        "CUS-001",
                        List.of(new OrderService.OrderLineRequest("PRD-001", 0)),
                        DiscountType.NO_DISCOUNT
                ));
        assertThrows(BusinessException.class, () ->
                container.getOrderService().createOrder(
                        "CUS-001",
                        List.of(new OrderService.OrderLineRequest("PRD-001", -3)),
                        DiscountType.NO_DISCOUNT
                ));
    }

    @Test
    void createProduct_withNegativePriceOrStock_throwsBusinessException() {
        assertThrows(BusinessException.class, () ->
                container.getProductService().createProduct(
                        "Test Mouse", "CAT-02", "Logitech",
                        new BigDecimal("-100000"), 5, "Desc", "", ProductStatus.ACTIVE
                ));
        assertThrows(BusinessException.class, () ->
                container.getProductService().createProduct(
                        "Test Mouse", "CAT-02", "Logitech",
                        new BigDecimal("500000"), -1, "Desc", "", ProductStatus.ACTIVE
                ));
    }

    @Test
    void payCancelledOrder_orAlreadyPaidOrder_throwsBusinessException() {
        Order order = container.getOrderService().createOrder(
                "CUS-001",
                List.of(new OrderService.OrderLineRequest("PRD-004", 1)),
                DiscountType.NO_DISCOUNT
        );
        container.getOrderService().cancelOrder(order.getId());
        assertEquals(OrderStatus.CANCELLED, container.getOrderService().getOrderById(order.getId()).getStatus());

        assertThrows(BusinessException.class, () ->
                container.getPaymentService().processPayment(order.getId(), PaymentMethod.MOCK_SUCCESS));

        Order order2 = container.getOrderService().createOrder(
                "CUS-001",
                List.of(new OrderService.OrderLineRequest("PRD-004", 1)),
                DiscountType.NO_DISCOUNT
        );
        container.getPaymentService().processPayment(order2.getId(), PaymentMethod.MOCK_SUCCESS);

        assertThrows(BusinessException.class, () ->
                container.getPaymentService().processPayment(order2.getId(), PaymentMethod.MOCK_SUCCESS));
        assertThrows(BusinessException.class, () ->
                container.getOrderService().cancelOrder(order2.getId()));
    }

    @Test
    void orderUnitPriceSnapshot_remainsUnchangedWhenProductPriceChangesLater() {
        Product product = container.getProductService().getProductById("PRD-001");
        BigDecimal priceAtOrderTime = product.getPrice();

        Order order = container.getOrderService().createOrder(
                "CUS-001",
                List.of(new OrderService.OrderLineRequest("PRD-001", 1)),
                DiscountType.NO_DISCOUNT
        );

        // Increase product price later
        container.getProductService().updateProduct(
                product.getId(), product.getName(), product.getCategoryId(), product.getBrand(),
                priceAtOrderTime.add(new BigDecimal("1000000")),
                product.getStockQuantity(), product.getDescription(), product.getImagePath(), product.getStatus()
        );

        Order savedOrder = container.getOrderService().getOrderById(order.getId());
        assertEquals(0, priceAtOrderTime.compareTo(savedOrder.getItems().get(0).getUnitPrice()));
        assertEquals(0, priceAtOrderTime.compareTo(savedOrder.getFinalTotal()));
    }

    @Test
    void normalCustomer_cannotApplyVipDiscount() {
        assertThrows(BusinessException.class, () ->
                container.getOrderService().createOrder(
                        "CUS-003", // NORMAL tier
                        List.of(new OrderService.OrderLineRequest("PRD-004", 1)),
                        DiscountType.VIP
                ));
    }

    @Test
    void jsonStorage_savesAndLoadsDataSafely_andHandlesCorruptFile(@TempDir Path tempDir) throws Exception {
        Path jsonPath = tempDir.resolve("test_store.json");
        AppConfig config = new AppConfig();
        config.setStoragePath(jsonPath);
        config.setAutoSaveEnabled(true);

        DependencyContainer diskContainer = new DependencyContainer(config, true);
        assertTrue(Files.exists(jsonPath));

        diskContainer.getProductService().createProduct(
                "Custom Mech Keyboard", "CAT-01", "Custom",
                new BigDecimal("3000000"), 10, "Custom", "", ProductStatus.ACTIVE
        );

        // Reload into fresh container
        DependencyContainer reloaded = new DependencyContainer(config, true);
        assertTrue(reloaded.getProductService().getAllProducts().stream()
                .anyMatch(p -> "Custom Mech Keyboard".equals(p.getName())));

        // Corrupt JSON file should fall back gracefully
        Files.writeString(jsonPath, "{ invalid json !!!");
        JSONStorageService storageService = new JSONStorageService(config);
        assertFalse(storageService.load(new InMemoryDataStore()));
    }
}
