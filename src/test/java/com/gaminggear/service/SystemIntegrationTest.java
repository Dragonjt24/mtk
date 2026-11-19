package com.gaminggear.service;

import com.gaminggear.config.AppConfig;
import com.gaminggear.config.DependencyContainer;
import com.gaminggear.model.Bill;
import com.gaminggear.model.Category;
import com.gaminggear.model.Product;
import com.gaminggear.model.User;
import com.gaminggear.model.enums.UserRole;
import com.gaminggear.patterns.factory.DiscountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SystemIntegrationTest {

    private DependencyContainer container;

    @BeforeEach
    void setUp() {
        AppConfig config = new AppConfig();
        config.setAutoSaveEnabled(false);
        container = new DependencyContainer(config, false);
    }

    @Test
    void sampleData_containsAll9CategoriesAndAtLeast20Products() {
        List<Category> categories = container.getCategoryService().getAllCategories();
        List<Product> products = container.getProductService().getAllProducts();

        Set<String> catNames = categories.stream().map(Category::getName).collect(Collectors.toSet());
        assertTrue(catNames.containsAll(List.of(
                "Keyboard", "Mouse", "Monitor", "Headset", "Speaker",
                "Webcam", "Controller", "Microphone", "Mouse Pad"
        )));
        assertTrue(products.size() >= 20, "Must have at least 20 sample products");
    }

    @Test
    void demoAdminLogin_succeedsWithAdminRole() {
        User admin = container.getAuthService().login("admin", "admin123");
        assertEquals("admin", admin.getUsername());
        assertEquals(UserRole.ADMIN, admin.getRole());
    }

    @Test
    void dashboardSummary_computesLiveMetricsAccurately() {
        DashboardService.DashboardSummary before = container.getDashboardService().getSummary();
        assertEquals(container.getProductService().getAllProducts().size(), before.totalProducts());
        assertEquals(container.getOrderService().getAllOrders().size(), before.totalOrders());
        assertTrue(before.totalRevenue().signum() > 0);

        // Creating a pending order increases totalOrders and pendingOrders dynamically
        container.getOrderService().createOrder(
                "CUS-001",
                List.of(new OrderService.OrderLineRequest("PRD-004", 1)),
                DiscountType.VIP
        );
        DashboardService.DashboardSummary after = container.getDashboardService().getSummary();
        assertEquals(before.totalOrders() + 1, after.totalOrders());
        assertEquals(before.pendingOrders() + 1, after.pendingOrders());
    }

    @Test
    void pdfInvoiceExporter_generatesNonEmptyPdfFile(@TempDir Path tempDir) throws Exception {
        Bill bill = container.getBillService().getAllBills().get(0);
        File pdfFile = tempDir.resolve(bill.getId() + ".pdf").toFile();

        container.getPdfInvoiceExporter().exportToPdf(bill, pdfFile);

        assertTrue(pdfFile.exists());
        assertTrue(Files.size(pdfFile.toPath()) > 500, "Generated PDF should be non-empty");
    }
}
