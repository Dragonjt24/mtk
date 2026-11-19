package com.gaminggear.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gaminggear.config.AppConfig;
import com.gaminggear.config.DependencyContainer;
import com.gaminggear.exception.BusinessException;
import com.gaminggear.exception.ResourceNotFoundException;
import com.gaminggear.model.Bill;
import com.gaminggear.model.Category;
import com.gaminggear.model.Customer;
import com.gaminggear.model.Order;
import com.gaminggear.model.Payment;
import com.gaminggear.model.Product;
import com.gaminggear.model.User;
import com.gaminggear.model.enums.MembershipTier;
import com.gaminggear.model.enums.OrderStatus;
import com.gaminggear.model.enums.PaymentMethod;
import com.gaminggear.model.enums.ProductStatus;
import com.gaminggear.model.enums.UserRole;
import com.gaminggear.patterns.factory.DiscountFactory;
import com.gaminggear.patterns.factory.DiscountType;
import com.gaminggear.patterns.strategy.DiscountStrategy;
import com.gaminggear.service.DashboardService;
import com.gaminggear.service.OrderService;
import com.gaminggear.service.ProductService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Lightweight JDK 17 HttpServer exposing the Java business services, InMemoryDataStore,
 * and Factory/Strategy/Observer patterns over localhost HTTP for the web interface.
 */
public class JavaApiBridgeServer {

    private final DependencyContainer container;
    private final ObjectMapper mapper;
    private HttpServer server;

    public JavaApiBridgeServer(DependencyContainer container) {
        this.container = container;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static void main(String[] args) throws IOException {
        int port = 8085;
        String envPort = System.getenv("JAVA_BRIDGE_PORT");
        if (envPort != null && !envPort.isBlank()) {
            port = Integer.parseInt(envPort.trim());
        }
        DependencyContainer container = new DependencyContainer();
        JavaApiBridgeServer bridge = new JavaApiBridgeServer(container);
        bridge.start(port);
        System.out.println("JavaApiBridgeServer running on http://127.0.0.1:" + port);
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.createContext("/api/java", this::handleRequest);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());

        try {
            if ("OPTIONS".equals(method)) {
                sendJson(exchange, 204, Map.of());
                return;
            }

            if ("/api/java/health".equals(path) && "GET".equals(method)) {
                sendJson(exchange, 200, Map.of("status", "ok", "engine", "Java 17"));
                return;
            }

            // AUTH
            if ("/api/java/auth/login".equals(path) && "POST".equals(method)) {
                JsonNode body = readJson(exchange);
                User user = container.getAuthService().login(text(body, "username"), text(body, "password"));
                sendJson(exchange, 200, sanitizeUser(user));
                return;
            }
            if ("/api/java/auth/logout".equals(path) && "POST".equals(method)) {
                container.getAuthService().logout();
                sendJson(exchange, 200, Map.of("loggedOut", true));
                return;
            }
            if ("/api/java/auth/users".equals(path) && "GET".equals(method)) {
                List<Map<String, Object>> users = container.getAuthService().getAllUsers().stream()
                        .map(this::sanitizeUser)
                        .toList();
                sendJson(exchange, 200, users);
                return;
            }
            if ("/api/java/auth/users".equals(path) && "POST".equals(method)) {
                JsonNode body = readJson(exchange);
                UserRole role = body.hasNonNull("role") ? UserRole.valueOf(text(body, "role")) : UserRole.STAFF;
                User created = container.getAuthService().createUser(
                        text(body, "username"), text(body, "password"), text(body, "fullName"), role);
                sendJson(exchange, 201, sanitizeUser(created));
                return;
            }
            if (path.startsWith("/api/java/auth/users/") && path.endsWith("/status") && "PUT".equals(method)) {
                String id = extractId(path, "/api/java/auth/users/", "/status");
                JsonNode body = readJson(exchange);
                UserRole role = body.hasNonNull("role") ? UserRole.valueOf(text(body, "role")) : null;
                boolean active = body.path("active").asBoolean(true);
                User updated = container.getAuthService().updateUserStatus(id, active, role, text(body, "fullName"));
                sendJson(exchange, 200, sanitizeUser(updated));
                return;
            }
            if (path.startsWith("/api/java/auth/users/") && path.endsWith("/password") && "PUT".equals(method)) {
                String id = extractId(path, "/api/java/auth/users/", "/password");
                JsonNode body = readJson(exchange);
                container.getAuthService().changePassword(id, text(body, "newPassword"));
                sendJson(exchange, 200, Map.of("updated", true));
                return;
            }

            // DASHBOARD
            if ("/api/java/dashboard".equals(path) && "GET".equals(method)) {
                DashboardService ds = container.getDashboardService();
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("summary", ds.getSummary());
                payload.put("revenueLastDays", ds.getRevenueLastDays(7));
                payload.put("revenueByCategory", ds.getRevenueByCategory());
                payload.put("orderStatusCounts", ds.getOrderCountsByStatus());
                payload.put("recentOrders", ds.getRecentOrders(8));
                payload.put("topProducts", ds.getTopSellingProducts(8));
                sendJson(exchange, 200, payload);
                return;
            }

            // CATEGORIES
            if ("/api/java/categories".equals(path) && "GET".equals(method)) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Category c : container.getCategoryService().getAllCategories()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", c.getId());
                    row.put("name", c.getName());
                    row.put("description", c.getDescription());
                    row.put("active", c.isActive());
                    row.put("productCount", container.getCategoryService().countProductsInCategory(c.getId()));
                    result.add(row);
                }
                sendJson(exchange, 200, result);
                return;
            }
            if ("/api/java/categories".equals(path) && "POST".equals(method)) {
                JsonNode body = readJson(exchange);
                Category created = container.getCategoryService().createCategory(
                        text(body, "name"), text(body, "description"), body.path("active").asBoolean(true));
                sendJson(exchange, 201, created);
                return;
            }
            if (path.startsWith("/api/java/categories/") && "PUT".equals(method)) {
                String id = path.substring("/api/java/categories/".length());
                JsonNode body = readJson(exchange);
                Category updated = container.getCategoryService().updateCategory(
                        id, text(body, "name"), text(body, "description"), body.path("active").asBoolean(true));
                sendJson(exchange, 200, updated);
                return;
            }
            if (path.startsWith("/api/java/categories/") && "DELETE".equals(method)) {
                String id = path.substring("/api/java/categories/".length());
                container.getCategoryService().deleteCategory(id);
                sendJson(exchange, 200, Map.of("deleted", true));
                return;
            }

            // PRODUCTS
            if ("/api/java/brands".equals(path) && "GET".equals(method)) {
                sendJson(exchange, 200, container.getProductService().getAllBrands());
                return;
            }
            if ("/api/java/products".equals(path) && "GET".equals(method)) {
                String keyword = query.getOrDefault("keyword", "");
                String categoryId = query.getOrDefault("categoryId", "");
                String brand = query.getOrDefault("brand", "");
                BigDecimal minPrice = parseOptionalBigDecimal(query.get("minPrice"));
                BigDecimal maxPrice = parseOptionalBigDecimal(query.get("maxPrice"));
                ProductService.SortOption sort = parseSort(query.get("sort"));
                List<Product> products = container.getProductService().searchAndFilter(
                        keyword, categoryId, brand, minPrice, maxPrice, sort);
                sendJson(exchange, 200, products);
                return;
            }
            if ("/api/java/products".equals(path) && "POST".equals(method)) {
                JsonNode body = readJson(exchange);
                Product created = container.getProductService().createProduct(
                        text(body, "name"),
                        text(body, "categoryId"),
                        text(body, "brand"),
                        parseRequiredBigDecimal(body, "price"),
                        body.path("stockQuantity").asInt(-1),
                        text(body, "description"),
                        text(body, "imagePath"),
                        parseProductStatus(body)
                );
                sendJson(exchange, 201, created);
                return;
            }
            if ("/api/java/products/batch-delete".equals(path) && "POST".equals(method)) {
                JsonNode body = readJson(exchange);
                List<String> ids = new ArrayList<>();
                if (body.has("ids") && body.get("ids").isArray()) {
                    for (JsonNode n : body.get("ids")) {
                        ids.add(n.asText());
                    }
                }
                int count = container.getProductService().deleteProducts(ids);
                sendJson(exchange, 200, Map.of("deletedCount", count));
                return;
            }
            if (path.startsWith("/api/java/products/") && "PUT".equals(method)) {
                String id = path.substring("/api/java/products/".length());
                JsonNode body = readJson(exchange);
                Product updated = container.getProductService().updateProduct(
                        id,
                        text(body, "name"),
                        text(body, "categoryId"),
                        text(body, "brand"),
                        parseRequiredBigDecimal(body, "price"),
                        body.path("stockQuantity").asInt(-1),
                        text(body, "description"),
                        text(body, "imagePath"),
                        parseProductStatus(body)
                );
                sendJson(exchange, 200, updated);
                return;
            }
            if (path.startsWith("/api/java/products/") && "DELETE".equals(method)) {
                String id = path.substring("/api/java/products/".length());
                container.getProductService().deleteProduct(id);
                sendJson(exchange, 200, Map.of("deleted", true));
                return;
            }

            // CUSTOMERS
            if ("/api/java/customers".equals(path) && "GET".equals(method)) {
                String keyword = query.getOrDefault("keyword", "");
                sendJson(exchange, 200, container.getCustomerService().searchCustomers(keyword));
                return;
            }
            if ("/api/java/customers".equals(path) && "POST".equals(method)) {
                JsonNode body = readJson(exchange);
                MembershipTier tier = body.hasNonNull("membershipTier")
                        ? MembershipTier.valueOf(text(body, "membershipTier"))
                        : MembershipTier.NORMAL;
                Customer created = container.getCustomerService().createCustomer(
                        text(body, "fullName"), text(body, "phone"),
                        text(body, "email"), text(body, "address"), tier);
                sendJson(exchange, 201, created);
                return;
            }
            if (path.startsWith("/api/java/customers/") && path.endsWith("/orders") && "GET".equals(method)) {
                String id = extractId(path, "/api/java/customers/", "/orders");
                sendJson(exchange, 200, container.getCustomerService().getPurchaseHistory(id));
                return;
            }
            if (path.startsWith("/api/java/customers/") && "PUT".equals(method)) {
                String id = path.substring("/api/java/customers/".length());
                JsonNode body = readJson(exchange);
                MembershipTier tier = body.hasNonNull("membershipTier")
                        ? MembershipTier.valueOf(text(body, "membershipTier"))
                        : MembershipTier.NORMAL;
                Customer updated = container.getCustomerService().updateCustomer(
                        id, text(body, "fullName"), text(body, "phone"),
                        text(body, "email"), text(body, "address"), tier);
                sendJson(exchange, 200, updated);
                return;
            }
            if (path.startsWith("/api/java/customers/") && "DELETE".equals(method)) {
                String id = path.substring("/api/java/customers/".length());
                container.getCustomerService().deleteCustomer(id);
                sendJson(exchange, 200, Map.of("deleted", true));
                return;
            }

            // DISCOUNTS (Factory + Strategy metadata)
            if ("/api/java/discounts".equals(path) && "GET".equals(method)) {
                List<Map<String, Object>> list = new ArrayList<>();
                for (DiscountType type : DiscountType.values()) {
                    DiscountStrategy strategy = DiscountFactory.createStrategy(type);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("type", type.name());
                    item.put("displayName", type.getDisplayName());
                    item.put("rate", strategy.getDiscountRate());
                    item.put("description", strategy.getDescription());
                    item.put("strategyClass", strategy.getClass().getSimpleName());
                    list.add(item);
                }
                sendJson(exchange, 200, list);
                return;
            }

            // ORDERS
            if ("/api/java/orders".equals(path) && "GET".equals(method)) {
                String statusStr = query.get("status");
                OrderStatus status = (statusStr == null || statusStr.isBlank() || "ALL".equalsIgnoreCase(statusStr))
                        ? null : OrderStatus.valueOf(statusStr.toUpperCase());
                sendJson(exchange, 200, container.getOrderService().getOrdersByStatus(status));
                return;
            }
            if ("/api/java/orders/preview-pricing".equals(path) && "POST".equals(method)) {
                JsonNode body = readJson(exchange);
                BigDecimal original = parseRequiredBigDecimal(body, "originalTotal");
                DiscountType type = parseDiscountType(text(body, "discountType"));
                sendJson(exchange, 200, container.getOrderService().previewPricing(original, type));
                return;
            }
            if ("/api/java/orders".equals(path) && "POST".equals(method)) {
                JsonNode body = readJson(exchange);
                String customerId = text(body, "customerId");
                DiscountType discountType = parseDiscountType(text(body, "discountType"));
                List<OrderService.OrderLineRequest> lines = new ArrayList<>();
                if (body.has("items") && body.get("items").isArray()) {
                    for (JsonNode n : body.get("items")) {
                        lines.add(new OrderService.OrderLineRequest(
                                text(n, "productId"),
                                n.path("quantity").asInt(0)
                        ));
                    }
                }
                Order created = container.getOrderService().createOrder(customerId, lines, discountType);
                sendJson(exchange, 201, created);
                return;
            }
            if (path.startsWith("/api/java/orders/") && path.endsWith("/discount") && "PUT".equals(method)) {
                String id = extractId(path, "/api/java/orders/", "/discount");
                JsonNode body = readJson(exchange);
                DiscountType discountType = parseDiscountType(text(body, "discountType"));
                Order updated = container.getOrderService().changeOrderDiscount(id, discountType);
                sendJson(exchange, 200, updated);
                return;
            }
            if (path.startsWith("/api/java/orders/") && path.endsWith("/cancel") && "POST".equals(method)) {
                String id = extractId(path, "/api/java/orders/", "/cancel");
                Order cancelled = container.getOrderService().cancelOrder(id);
                sendJson(exchange, 200, cancelled);
                return;
            }

            // PAYMENTS & NOTIFICATIONS (Observer Pattern)
            if ("/api/java/payments".equals(path) && "GET".equals(method)) {
                sendJson(exchange, 200, container.getPaymentService().getAllPayments());
                return;
            }
            if ("/api/java/payments/process".equals(path) && "POST".equals(method)) {
                JsonNode body = readJson(exchange);
                String orderId = text(body, "orderId");
                PaymentMethod pm = PaymentMethod.valueOf(text(body, "method"));
                Payment payment = container.getPaymentService().processPayment(orderId, pm);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("payment", payment);
                result.put("order", container.getOrderService().getOrderById(orderId));
                result.put("bill", container.getBillService().findByOrderId(orderId).orElse(null));
                sendJson(exchange, 200, result);
                return;
            }
            if ("/api/java/notifications".equals(path) && "GET".equals(method)) {
                sendJson(exchange, 200, container.getNotificationObserver().getNotifications());
                return;
            }

            // BILLS / INVOICES & PDF EXPORT
            if ("/api/java/bills".equals(path) && "GET".equals(method)) {
                sendJson(exchange, 200, container.getBillService().getAllBills());
                return;
            }
            if (path.startsWith("/api/java/bills/") && path.endsWith("/pdf") && "GET".equals(method)) {
                String id = extractId(path, "/api/java/bills/", "/pdf");
                Bill bill = container.getBillService().getBillById(id);
                Path tempPdf = Files.createTempFile("invoice_" + bill.getId() + "_", ".pdf");
                try {
                    container.getPdfInvoiceExporter().exportToPdf(bill, tempPdf.toFile());
                    byte[] pdfBytes = Files.readAllBytes(tempPdf);
                    exchange.getResponseHeaders().set("Content-Type", "application/pdf");
                    exchange.getResponseHeaders().set("Content-Disposition",
                            "attachment; filename=\"" + bill.getId() + ".pdf\"");
                    exchange.sendResponseHeaders(200, pdfBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(pdfBytes);
                    }
                } finally {
                    Files.deleteIfExists(tempPdf);
                }
                return;
            }

            // INVENTORY
            if ("/api/java/inventory/low-stock".equals(path) && "GET".equals(method)) {
                sendJson(exchange, 200, container.getInventoryService().getLowStockProducts());
                return;
            }
            if ("/api/java/inventory/logs".equals(path) && "GET".equals(method)) {
                sendJson(exchange, 200, container.getInventoryService().getAllLogs());
                return;
            }
            if ("/api/java/inventory/adjust".equals(path) && "POST".equals(method)) {
                JsonNode body = readJson(exchange);
                String productId = text(body, "productId");
                String mode = text(body, "mode");
                int amount = body.path("amount").asInt(0);
                String reason = text(body, "reason");
                Product updated = "SET".equalsIgnoreCase(mode)
                        ? container.getInventoryService().setStockQuantity(productId, amount, reason)
                        : container.getInventoryService().adjustStock(productId, amount, reason);
                sendJson(exchange, 200, updated);
                return;
            }

            // SETTINGS & JSON STORAGE
            if ("/api/java/settings".equals(path) && "GET".equals(method)) {
                AppConfig cfg = container.getAppConfig();
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("lowStockThreshold", cfg.getLowStockThreshold());
                data.put("autoSaveEnabled", cfg.isAutoSaveEnabled());
                data.put("storagePath", cfg.getStoragePath().toAbsolutePath().toString());
                data.put("storeName", AppConfig.STORE_NAME);
                data.put("storeAddress", AppConfig.STORE_ADDRESS);
                data.put("storePhone", AppConfig.STORE_PHONE);
                sendJson(exchange, 200, data);
                return;
            }
            if ("/api/java/settings".equals(path) && "PUT".equals(method)) {
                JsonNode body = readJson(exchange);
                if (body.has("lowStockThreshold")) {
                    container.getAppConfig().setLowStockThreshold(body.path("lowStockThreshold").asInt());
                }
                if (body.has("autoSaveEnabled")) {
                    container.getAppConfig().setAutoSaveEnabled(body.path("autoSaveEnabled").asBoolean());
                }
                sendJson(exchange, 200, Map.of("updated", true));
                return;
            }
            if ("/api/java/settings/save".equals(path) && "POST".equals(method)) {
                container.saveNow();
                sendJson(exchange, 200, Map.of("saved", true));
                return;
            }
            if ("/api/java/settings/reload".equals(path) && "POST".equals(method)) {
                boolean loaded = container.reloadFromJson();
                sendJson(exchange, 200, Map.of("reloaded", loaded));
                return;
            }
            if ("/api/java/settings/reset".equals(path) && "POST".equals(method)) {
                container.resetToSampleData();
                sendJson(exchange, 200, Map.of("reset", true));
                return;
            }

            sendJson(exchange, 404, Map.of("error", "Không tìm thấy endpoint: " + path));
        } catch (BusinessException | IllegalArgumentException ex) {
            sendJson(exchange, 400, Map.of("error", ex.getMessage()));
        } catch (ResourceNotFoundException ex) {
            sendJson(exchange, 404, Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            sendJson(exchange, 500, Map.of("error", "Lỗi máy chủ Java: " + ex.getMessage()));
        }
    }

    private Map<String, Object> sanitizeUser(User u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("username", u.getUsername());
        map.put("fullName", u.getFullName());
        map.put("role", u.getRole().name());
        map.put("active", u.isActive());
        return map;
    }

    private JsonNode readJson(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while ((n = is.read(chunk)) != -1) {
                buffer.write(chunk, 0, n);
            }
            String raw = buffer.toString(StandardCharsets.UTF_8);
            if (raw.isBlank()) {
                return mapper.createObjectNode();
            }
            return mapper.readTree(raw);
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        byte[] bytes = statusCode == 204 ? new byte[0] : mapper.writeValueAsBytes(payload);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, statusCode == 204 ? -1 : bytes.length);
        if (statusCode != 204) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return "";
        }
        return node.get(field).asText("");
    }

    private BigDecimal parseRequiredBigDecimal(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            throw new BusinessException("Thiếu trường số tiền: " + field);
        }
        try {
            return new BigDecimal(node.get(field).asText().trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException("Giá trị số không hợp lệ cho " + field);
        }
    }

    private BigDecimal parseOptionalBigDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException("Giá trị lọc số không hợp lệ: " + raw);
        }
    }

    private ProductService.SortOption parseSort(String raw) {
        if (raw == null || raw.isBlank()) {
            return ProductService.SortOption.NAME_ASC;
        }
        try {
            return ProductService.SortOption.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ProductService.SortOption.NAME_ASC;
        }
    }

    private ProductStatus parseProductStatus(JsonNode node) {
        String raw = text(node, "status");
        if (raw.isBlank()) {
            return ProductStatus.ACTIVE;
        }
        return ProductStatus.valueOf(raw.trim().toUpperCase());
    }

    private DiscountType parseDiscountType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return DiscountType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Loại chính sách giảm giá không hợp lệ: " + raw);
        }
    }

    private String extractId(String path, String prefix, String suffix) {
        return path.substring(prefix.length(), path.length() - suffix.length());
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return map;
        }
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(k, v);
            }
        }
        return map;
    }
}
