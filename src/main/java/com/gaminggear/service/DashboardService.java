package com.gaminggear.service;

import com.gaminggear.config.AppConfig;
import com.gaminggear.model.Category;
import com.gaminggear.model.Order;
import com.gaminggear.model.OrderItem;
import com.gaminggear.model.Product;
import com.gaminggear.model.enums.OrderStatus;
import com.gaminggear.model.enums.ProductStatus;
import com.gaminggear.repository.CategoryRepository;
import com.gaminggear.repository.OrderRepository;
import com.gaminggear.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Computes real-time dashboard statistics and chart datasets dynamically from in-memory data.
 */
public class DashboardService {

    public record DashboardSummary(
            int totalProducts,
            int totalOrders,
            BigDecimal totalRevenue,
            int pendingOrders,
            int lowStockProducts
    ) {
    }

    public record TopSellingProduct(
            String productId,
            String productName,
            int totalQuantitySold,
            BigDecimal totalRevenue
    ) {
    }

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final AppConfig appConfig;

    public DashboardService(ProductRepository productRepository,
                            CategoryRepository categoryRepository,
                            OrderRepository orderRepository,
                            AppConfig appConfig) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.orderRepository = orderRepository;
        this.appConfig = appConfig;
    }

    public DashboardSummary getSummary() {
        List<Product> products = productRepository.findAll();
        List<Order> orders = orderRepository.findAll();

        BigDecimal totalRevenue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .map(Order::getFinalTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int pendingOrders = (int) orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .count();

        int threshold = appConfig.getLowStockThreshold();
        int lowStockCount = (int) products.stream()
                .filter(p -> p.getStatus() != ProductStatus.INACTIVE)
                .filter(p -> p.getStockQuantity() <= threshold)
                .count();

        return new DashboardSummary(
                products.size(),
                orders.size(),
                totalRevenue,
                pendingOrders,
                lowStockCount
        );
    }

    public Map<OrderStatus, Integer> getOrderCountsByStatus() {
        Map<OrderStatus, Integer> counts = new LinkedHashMap<>();
        counts.put(OrderStatus.PENDING, 0);
        counts.put(OrderStatus.PAID, 0);
        counts.put(OrderStatus.CANCELLED, 0);

        for (Order order : orderRepository.findAll()) {
            counts.merge(order.getStatus(), 1, Integer::sum);
        }
        return counts;
    }

    public Map<String, BigDecimal> getRevenueByCategory() {
        Map<String, String> catNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        Map<String, String> productToCat = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Product::getCategoryId, (a, b) -> a));

        Map<String, BigDecimal> revenueByCategory = new LinkedHashMap<>();
        for (Order order : orderRepository.findAll()) {
            if (order.getStatus() != OrderStatus.PAID) {
                continue;
            }
            for (OrderItem item : order.getItems()) {
                String catId = productToCat.get(item.getProductId());
                String catName = catId != null ? catNames.getOrDefault(catId, "Khác") : "Khác";
                revenueByCategory.merge(catName, item.getLineTotal(), BigDecimal::add);
            }
        }
        return revenueByCategory;
    }

    public Map<String, BigDecimal> getRevenueLastDays(int days) {
        Map<String, BigDecimal> daily = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            daily.put(d.format(fmt), BigDecimal.ZERO);
        }
        for (Order order : orderRepository.findAll()) {
            if (order.getStatus() != OrderStatus.PAID || order.getCreatedAt() == null) {
                continue;
            }
            String key = order.getCreatedAt().toLocalDate().format(fmt);
            if (daily.containsKey(key)) {
                daily.merge(key, order.getFinalTotal(), BigDecimal::add);
            }
        }
        return daily;
    }

    public List<Order> getRecentOrders(int limit) {
        return orderRepository.findAll().stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<TopSellingProduct> getTopSellingProducts(int limit) {
        Map<String, TopSellingProduct> map = new LinkedHashMap<>();
        for (Order order : orderRepository.findAll()) {
            if (order.getStatus() != OrderStatus.PAID) {
                continue;
            }
            for (OrderItem item : order.getItems()) {
                TopSellingProduct existing = map.get(item.getProductId());
                if (existing == null) {
                    map.put(item.getProductId(), new TopSellingProduct(
                            item.getProductId(),
                            item.getProductName(),
                            item.getQuantity(),
                            item.getLineTotal()
                    ));
                } else {
                    map.put(item.getProductId(), new TopSellingProduct(
                            item.getProductId(),
                            item.getProductName(),
                            existing.totalQuantitySold() + item.getQuantity(),
                            existing.totalRevenue().add(item.getLineTotal())
                    ));
                }
            }
        }
        List<TopSellingProduct> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparingInt(TopSellingProduct::totalQuantitySold).reversed()
                .thenComparing(TopSellingProduct::totalRevenue, Comparator.reverseOrder()));
        return list.stream().limit(limit).collect(Collectors.toList());
    }
}
