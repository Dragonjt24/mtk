package com.gaminggear.patterns;

import com.gaminggear.config.AppConfig;
import com.gaminggear.config.DependencyContainer;
import com.gaminggear.model.Order;
import com.gaminggear.model.Payment;
import com.gaminggear.model.Product;
import com.gaminggear.model.enums.OrderStatus;
import com.gaminggear.model.enums.PaymentMethod;
import com.gaminggear.model.enums.PaymentStatus;
import com.gaminggear.patterns.factory.DiscountType;
import com.gaminggear.patterns.observer.PaymentEvent;
import com.gaminggear.patterns.observer.PaymentObserver;
import com.gaminggear.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PaymentObserverTest {

    private DependencyContainer container;

    @BeforeEach
    void setUp() {
        AppConfig config = new AppConfig();
        config.setAutoSaveEnabled(false);
        container = new DependencyContainer(config, false);
    }

    @Test
    void paymentSuccess_publishesEventAndTriggersBillInventoryAndNotificationObservers() {
        Product product = container.getProductService().getProductById("PRD-004");
        int stockBefore = product.getStockQuantity();

        Order order = container.getOrderService().createOrder(
                "CUS-001",
                List.of(new OrderService.OrderLineRequest("PRD-004", 2)),
                DiscountType.VIP
        );

        Payment payment = container.getPaymentService().processPayment(order.getId(), PaymentMethod.MOCK_SUCCESS);

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(OrderStatus.PAID, container.getOrderService().getOrderById(order.getId()).getStatus());

        // BillObserver created bill
        assertTrue(container.getBillService().findByOrderId(order.getId()).isPresent());

        // InventoryObserver deducted stock exactly once
        int stockAfter = container.getProductService().getProductById("PRD-004").getStockQuantity();
        assertEquals(stockBefore - 2, stockAfter);

        // NotificationObserver received notification
        assertFalse(container.getNotificationObserver().getNotifications().isEmpty());
        assertTrue(container.getNotificationObserver().getNotifications().get(0).contains(order.getId()));
    }

    @Test
    void paymentFailure_doesNotPublishSuccessEvent_doesNotCreateBill_doesNotDeductStock() {
        Product product = container.getProductService().getProductById("PRD-004");
        int stockBefore = product.getStockQuantity();
        int billsBefore = container.getBillService().getAllBills().size();
        int notificationsBefore = container.getNotificationObserver().getNotifications().size();

        Order order = container.getOrderService().createOrder(
                "CUS-001",
                List.of(new OrderService.OrderLineRequest("PRD-004", 2)),
                DiscountType.NO_DISCOUNT
        );

        Payment payment = container.getPaymentService().processPayment(order.getId(), PaymentMethod.MOCK_FAILURE);

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals(OrderStatus.PENDING, container.getOrderService().getOrderById(order.getId()).getStatus());
        assertTrue(container.getBillService().findByOrderId(order.getId()).isEmpty());
        assertEquals(billsBefore, container.getBillService().getAllBills().size());
        assertEquals(stockBefore, container.getProductService().getProductById("PRD-004").getStockQuantity());
        assertEquals(notificationsBefore, container.getNotificationObserver().getNotifications().size());
    }

    @Test
    void unregisterObserver_stopsReceivingPaymentEvents() {
        AtomicInteger callCount = new AtomicInteger(0);
        PaymentObserver customObserver = event -> callCount.incrementAndGet();

        container.getPaymentEventPublisher().registerObserver(customObserver);

        Order order1 = container.getOrderService().createOrder(
                "CUS-001",
                List.of(new OrderService.OrderLineRequest("PRD-021", 1)),
                DiscountType.NO_DISCOUNT
        );
        container.getPaymentService().processPayment(order1.getId(), PaymentMethod.MOCK_SUCCESS);
        assertEquals(1, callCount.get());

        // Unregister and process another order
        container.getPaymentEventPublisher().unregisterObserver(customObserver);

        Order order2 = container.getOrderService().createOrder(
                "CUS-001",
                List.of(new OrderService.OrderLineRequest("PRD-021", 1)),
                DiscountType.NO_DISCOUNT
        );
        container.getPaymentService().processPayment(order2.getId(), PaymentMethod.COD);
        assertEquals(1, callCount.get());
    }

    @Test
    void duplicatePaymentEvent_doesNotCreateDuplicateBillOrDeductStockTwice() {
        Product product = container.getProductService().getProductById("PRD-005");
        int stockBefore = product.getStockQuantity();

        Order order = container.getOrderService().createOrder(
                "CUS-002",
                List.of(new OrderService.OrderLineRequest("PRD-005", 3)),
                DiscountType.SEASONAL
        );
        Payment payment = container.getPaymentService().processPayment(order.getId(), PaymentMethod.MOCK_SUCCESS);
        int billsAfterFirst = container.getBillService().getAllBills().size();

        // Simulate accidental duplicate event broadcast
        PaymentEvent duplicateEvent = new PaymentEvent("EVT-DUP-1", order, payment, LocalDateTime.now());
        container.getPaymentEventPublisher().notifyObservers(duplicateEvent);

        assertEquals(billsAfterFirst, container.getBillService().getAllBills().size());
        assertEquals(stockBefore - 3, container.getProductService().getProductById("PRD-005").getStockQuantity());
    }
}
