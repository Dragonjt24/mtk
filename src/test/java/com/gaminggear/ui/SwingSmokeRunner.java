package com.gaminggear.ui;

import com.gaminggear.config.AppConfig;
import com.gaminggear.config.DependencyContainer;
import com.gaminggear.model.Bill;
import com.gaminggear.model.Order;
import com.gaminggear.model.Payment;
import com.gaminggear.model.Product;
import com.gaminggear.model.User;
import com.gaminggear.model.enums.PaymentMethod;
import com.gaminggear.patterns.factory.DiscountType;
import com.gaminggear.service.DashboardService;
import com.gaminggear.service.OrderService;
import com.gaminggear.ui.theme.GamingTheme;
import com.gaminggear.util.CurrencyUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Live Swing GUI & Business Flow verification runner under Xvfb.
 */
public class SwingSmokeRunner {

    public static void main(String[] args) throws Exception {
        Path outDir = Path.of("target", "preview");
        Files.createDirectories(outDir);

        GamingTheme.setup();
        AppConfig config = new AppConfig();
        config.setAutoSaveEnabled(false);
        DependencyContainer container = new DependencyContainer(config, false);

        System.out.println("=== GAMING GEAR MANAGEMENT SYSTEM - LIVE EXECUTION DEMO ===");
        System.out.println("1. Khoi tao du lieu mau: "
                + container.getCategoryService().getAllCategories().size() + " danh muc, "
                + container.getProductService().getAllProducts().size() + " san pham, "
                + container.getCustomerService().getAllCustomers().size() + " khach hang.");

        User admin = container.getAuthService().login("admin", "admin123");
        System.out.println("2. Dang nhap thanh cong: " + admin.getUsername() + " [" + admin.getRole() + "]");

        // Live Factory + Strategy + Observer execution
        Product mouse = container.getProductService().getProductById("PRD-004");
        int stockBefore = mouse.getStockQuantity();
        Order newOrder = container.getOrderService().createOrder(
                "CUS-001",
                List.of(new OrderService.OrderLineRequest("PRD-004", 2)),
                DiscountType.VIP
        );
        System.out.println("3. Tao don hang (Factory + VIP Strategy 15%): " + newOrder.getId()
                + " | Gia goc: " + CurrencyUtil.formatVnd(newOrder.getOriginalTotal())
                + " | Giam: -" + CurrencyUtil.formatVnd(newOrder.getDiscountAmount())
                + " | Thanh toan: " + CurrencyUtil.formatVnd(newOrder.getFinalTotal())
                + " | Trang thai: " + newOrder.getStatus());

        Payment failPay = container.getPaymentService().processPayment(newOrder.getId(), PaymentMethod.MOCK_FAILURE);
        System.out.println("4. Thu thanh toan MOCK_FAILURE -> Ket qua: " + failPay.getStatus()
                + " | Don hang van la: " + container.getOrderService().getOrderById(newOrder.getId()).getStatus()
                + " | Ton kho PRD-004 van la: " + container.getProductService().getProductById("PRD-004").getStockQuantity());

        Payment okPay = container.getPaymentService().processPayment(newOrder.getId(), PaymentMethod.MOCK_SUCCESS);
        Bill createdBill = container.getBillService().findByOrderId(newOrder.getId()).orElseThrow();
        int stockAfter = container.getProductService().getProductById("PRD-004").getStockQuantity();
        System.out.println("5. Thanh toan MOCK_SUCCESS -> Ket qua: " + okPay.getStatus()
                + " | Trang thai don: " + container.getOrderService().getOrderById(newOrder.getId()).getStatus()
                + " | BillObserver tao hoa don: " + createdBill.getId()
                + " | InventoryObserver tru kho PRD-004: " + stockBefore + " -> " + stockAfter);

        File pdfOut = outDir.resolve(createdBill.getId() + ".pdf").toFile();
        container.getPdfInvoiceExporter().exportToPdf(createdBill, pdfOut);
        System.out.println("6. Xuat hoa don PDF: " + pdfOut.getPath() + " (" + pdfOut.length() + " bytes)");

        DashboardService.DashboardSummary summary = container.getDashboardService().getSummary();
        System.out.println("7. Dashboard KPIs -> SP: " + summary.totalProducts()
                + " | Don hang: " + summary.totalOrders()
                + " | Doanh thu: " + CurrencyUtil.formatVnd(summary.totalRevenue())
                + " | Cho xu ly: " + summary.pendingOrders()
                + " | Sap het hang: " + summary.lowStockProducts());

        // Launch real Swing frames in Xvfb and capture screenshots
        SwingUtilities.invokeAndWait(() -> {
            try {
                LoginFrame loginFrame = new LoginFrame(container);
                loginFrame.setVisible(true);
                loginFrame.validate();
                Thread.sleep(250);
                captureWindow(loginFrame, outDir.resolve("01_login.png").toFile());
                loginFrame.dispose();

                MainFrame mainFrame = new MainFrame(container, admin);
                mainFrame.setVisible(true);
                mainFrame.validate();

                String[][] tabs = {
                        {"DASHBOARD", "02_dashboard.png"},
                        {"PRODUCTS", "03_products.png"},
                        {"CREATE_ORDER", "04_create_order.png"},
                        {"PAYMENTS", "05_payments.png"},
                        {"INVOICES", "06_invoices.png"}
                };
                for (String[] tab : tabs) {
                    mainFrame.navigateTo(tab[0]);
                    mainFrame.validate();
                    mainFrame.repaint();
                    Thread.sleep(250);
                    captureWindow(mainFrame, outDir.resolve(tab[1]).toFile());
                }
                mainFrame.dispose();
                System.out.println("8. Swing GUI render & chup man hinh thanh cong 6 man hinh tai target/preview/");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private static void captureWindow(Window window, File outputFile) throws Exception {
        int w = Math.max(1, window.getWidth());
        int h = Math.max(1, window.getHeight());
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        window.paintAll(g2);
        g2.dispose();
        ImageIO.write(img, "png", outputFile);
    }
}
