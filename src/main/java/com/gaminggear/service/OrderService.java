package com.gaminggear.service;

import com.gaminggear.exception.BusinessException;
import com.gaminggear.exception.ResourceNotFoundException;
import com.gaminggear.model.Customer;
import com.gaminggear.model.Order;
import com.gaminggear.model.OrderItem;
import com.gaminggear.model.Product;
import com.gaminggear.model.enums.MembershipTier;
import com.gaminggear.model.enums.OrderStatus;
import com.gaminggear.model.enums.ProductStatus;
import com.gaminggear.patterns.factory.DiscountFactory;
import com.gaminggear.patterns.factory.DiscountType;
import com.gaminggear.patterns.strategy.DiscountContext;
import com.gaminggear.patterns.strategy.DiscountStrategy;
import com.gaminggear.repository.CustomerRepository;
import com.gaminggear.repository.OrderRepository;
import com.gaminggear.repository.ProductRepository;
import com.gaminggear.storage.StorageSync;
import com.gaminggear.util.CurrencyUtil;
import com.gaminggear.util.IdGenerator;
import com.gaminggear.util.ValidationUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OrderService {

    public record OrderLineRequest(String productId, int quantity) {
    }

    public record PricingResult(
            DiscountType discountType,
            BigDecimal discountRate,
            BigDecimal originalTotal,
            BigDecimal discountAmount,
            BigDecimal finalTotal,
            String strategyDescription
    ) {
    }

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final DiscountContext discountContext;
    private final StorageSync storageSync;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository,
                        DiscountContext discountContext,
                        StorageSync storageSync) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.discountContext = discountContext;
        this.storageSync = storageSync;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll().stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        if (status == null) {
            return getAllOrders();
        }
        return getAllOrders().stream()
                .filter(o -> o.getStatus() == status)
                .collect(Collectors.toList());
    }

    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));
    }

    public PricingResult previewPricing(BigDecimal originalTotal, DiscountType discountType) {
        BigDecimal safeOriginal = CurrencyUtil.normalize(
                ValidationUtil.requireNonNegative(originalTotal, "Tổng tiền ban đầu"));
        DiscountStrategy strategy = DiscountFactory.createStrategy(discountType);
        discountContext.setStrategy(strategy);
        BigDecimal discountAmount = discountContext.calculateDiscountAmount(safeOriginal);
        BigDecimal finalTotal = discountContext.calculateFinalTotal(safeOriginal);
        return new PricingResult(
                strategy.getDiscountType(),
                strategy.getDiscountRate(),
                safeOriginal,
                discountAmount,
                finalTotal,
                strategy.getDescription()
        );
    }

    public Order createOrder(String customerId, List<OrderLineRequest> lineRequests, DiscountType discountType) {
        ValidationUtil.requireNonBlank(customerId, "Khách hàng");
        if (lineRequests == null || lineRequests.isEmpty()) {
            throw new BusinessException("Không thể tạo đơn hàng với giỏ hàng trống.");
        }
        if (discountType == null) {
            throw new BusinessException("Vui lòng chọn chính sách giảm giá hợp lệ.");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng: " + customerId));

        validateDiscountEligibility(customer, discountType);

        // Merge quantities by productId to enforce stock limits accurately
        Map<String, Integer> aggregatedQty = new LinkedHashMap<>();
        for (OrderLineRequest req : lineRequests) {
            if (req == null || req.productId() == null || req.productId().isBlank()) {
                throw new BusinessException("Thông tin sản phẩm trong giỏ hàng không hợp lệ.");
            }
            ValidationUtil.requirePositiveInt(req.quantity(), "Số lượng sản phẩm");
            aggregatedQty.merge(req.productId(), req.quantity(), Integer::sum);
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal originalTotal = BigDecimal.ZERO;

        for (Map.Entry<String, Integer> entry : aggregatedQty.entrySet()) {
            Product product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + entry.getKey()));
            if (product.getStatus() == ProductStatus.INACTIVE) {
                throw new BusinessException("Sản phẩm '" + product.getName() + "' đã ngừng kinh doanh.");
            }
            int qty = entry.getValue();
            if (qty > product.getStockQuantity()) {
                throw new BusinessException("Sản phẩm '" + product.getName() + "' chỉ còn "
                        + product.getStockQuantity() + " sản phẩm trong kho.");
            }
            BigDecimal snapshotPrice = CurrencyUtil.normalize(product.getPrice());
            OrderItem item = new OrderItem(product.getId(), product.getName(), qty, snapshotPrice);
            orderItems.add(item);
            originalTotal = originalTotal.add(item.getLineTotal());
        }

        PricingResult pricing = previewPricing(originalTotal, discountType);

        Order order = new Order(
                IdGenerator.nextId("ORD"),
                customer.getId(),
                customer.getFullName(),
                orderItems,
                pricing.discountType(),
                pricing.discountRate(),
                pricing.originalTotal(),
                pricing.discountAmount(),
                pricing.finalTotal(),
                LocalDateTime.now(),
                OrderStatus.PENDING
        );

        orderRepository.save(order);
        persist();
        return order;
    }

    public Order changeOrderDiscount(String orderId, DiscountType newDiscountType) {
        Order order = getOrderById(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Chỉ có thể thay đổi chính sách giảm giá cho đơn hàng ở trạng thái PENDING.");
        }
        if (newDiscountType == null) {
            throw new BusinessException("Chính sách giảm giá không hợp lệ.");
        }
        Customer customer = customerRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng: " + order.getCustomerId()));
        validateDiscountEligibility(customer, newDiscountType);

        PricingResult pricing = previewPricing(order.getOriginalTotal(), newDiscountType);
        order.setDiscountType(pricing.discountType());
        order.setDiscountRate(pricing.discountRate());
        order.setDiscountAmount(pricing.discountAmount());
        order.setFinalTotal(pricing.finalTotal());
        orderRepository.save(order);
        persist();
        return order;
    }

    public Order cancelOrder(String orderId) {
        Order order = getOrderById(orderId);
        if (order.getStatus() == OrderStatus.PAID) {
            throw new BusinessException("Không thể hủy đơn hàng đã thanh toán.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Đơn hàng này đã bị hủy trước đó.");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        persist();
        return order;
    }

    private void validateDiscountEligibility(Customer customer, DiscountType discountType) {
        if (discountType == DiscountType.VIP && customer.getMembershipTier() != MembershipTier.VIP) {
            throw new BusinessException("Chính sách giảm giá VIP (15%) chỉ áp dụng cho khách hàng hạng VIP.");
        }
    }

    private void persist() {
        if (storageSync != null) {
            storageSync.persistIfEnabled();
        }
    }
}
