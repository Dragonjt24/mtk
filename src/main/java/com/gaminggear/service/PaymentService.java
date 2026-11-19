package com.gaminggear.service;

import com.gaminggear.exception.BusinessException;
import com.gaminggear.exception.ResourceNotFoundException;
import com.gaminggear.model.Order;
import com.gaminggear.model.Payment;
import com.gaminggear.model.enums.OrderStatus;
import com.gaminggear.model.enums.PaymentMethod;
import com.gaminggear.model.enums.PaymentStatus;
import com.gaminggear.patterns.observer.PaymentEvent;
import com.gaminggear.patterns.observer.PaymentEventPublisher;
import com.gaminggear.repository.OrderRepository;
import com.gaminggear.repository.PaymentRepository;
import com.gaminggear.storage.StorageSync;
import com.gaminggear.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;
    private final CustomerService customerService;
    private final PaymentEventPublisher eventPublisher;
    private final StorageSync storageSync;

    public PaymentService(OrderRepository orderRepository,
                          PaymentRepository paymentRepository,
                          InventoryService inventoryService,
                          CustomerService customerService,
                          PaymentEventPublisher eventPublisher,
                          StorageSync storageSync) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.inventoryService = inventoryService;
        this.customerService = customerService;
        this.eventPublisher = eventPublisher;
        this.storageSync = storageSync;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll().stream()
                .sorted(Comparator.comparing(Payment::getPaidAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public synchronized Payment processPayment(String orderId, PaymentMethod method) {
        if (method == null) {
            throw new BusinessException("Vui lòng chọn phương thức thanh toán.");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        if (order.getStatus() == OrderStatus.PAID) {
            throw new BusinessException("Đơn hàng " + orderId + " đã được thanh toán trước đó.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Không thể thanh toán đơn hàng đã bị hủy (" + orderId + ").");
        }

        // Verify stock before attempting payment
        inventoryService.validateStockForOrder(order);

        LocalDateTime now = LocalDateTime.now();

        if (method == PaymentMethod.MOCK_FAILURE) {
            Payment failedPayment = new Payment(
                    IdGenerator.nextId("PAY"),
                    order.getId(),
                    method,
                    order.getFinalTotal(),
                    PaymentStatus.FAILED,
                    now,
                    "Thanh toán thất bại (mô phỏng lỗi cổng thanh toán). Đơn hàng giữ nguyên trạng thái PENDING."
            );
            paymentRepository.save(failedPayment);
            persist();
            return failedPayment;
        }

        // Confirmed success (COD collected or Mock Success)
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        customerService.addTotalSpent(order.getCustomerId(), order.getFinalTotal());

        String successMessage = method == PaymentMethod.COD
                ? "Thanh toán COD thành công."
                : "Thanh toán trực tuyến mô phỏng thành công.";

        Payment successPayment = new Payment(
                IdGenerator.nextId("PAY"),
                order.getId(),
                method,
                order.getFinalTotal(),
                PaymentStatus.SUCCESS,
                now,
                successMessage
        );
        paymentRepository.save(successPayment);

        // Publish PaymentSuccess event ONLY after payment confirmation
        PaymentEvent event = new PaymentEvent(
                IdGenerator.nextId("EVT"),
                order,
                successPayment,
                now
        );
        eventPublisher.notifyObservers(event);

        persist();
        return successPayment;
    }

    private void persist() {
        if (storageSync != null) {
            storageSync.persistIfEnabled();
        }
    }
}
