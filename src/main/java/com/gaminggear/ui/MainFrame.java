package com.gaminggear.ui;

import com.gaminggear.config.AppConfig;
import com.gaminggear.config.DependencyContainer;
import com.gaminggear.model.User;
import com.gaminggear.ui.category.CategoryPanel;
import com.gaminggear.ui.components.HeaderPanel;
import com.gaminggear.ui.components.RefreshablePanel;
import com.gaminggear.ui.components.SidebarPanel;
import com.gaminggear.ui.customer.CustomerPanel;
import com.gaminggear.ui.dashboard.DashboardPanel;
import com.gaminggear.ui.discount.DiscountPanel;
import com.gaminggear.ui.inventory.InventoryPanel;
import com.gaminggear.ui.invoice.InvoicePanel;
import com.gaminggear.ui.order.CreateOrderPanel;
import com.gaminggear.ui.order.OrderListPanel;
import com.gaminggear.ui.payment.PaymentPanel;
import com.gaminggear.ui.product.ProductPanel;
import com.gaminggear.ui.settings.SettingsPanel;
import com.gaminggear.ui.theme.GamingTheme;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Primary Desktop Frame hosting Sidebar, Header, and CardLayout switching across all 11 business screens.
 */
public class MainFrame extends JFrame {

    private final DependencyContainer container;
    private final SidebarPanel sidebarPanel;
    private final HeaderPanel headerPanel;
    private final CardLayout cardLayout;
    private final JPanel contentContainer;
    private final JLabel toastBannerLabel;

    private final Map<String, RefreshablePanel> screens = new LinkedHashMap<>();
    private final Map<String, String> screenTitles = new LinkedHashMap<>();

    private PaymentPanel paymentPanel;
    private InvoicePanel invoicePanel;
    private String currentKey = "DASHBOARD";

    public MainFrame(DependencyContainer container, User loggedInUser) {
        this.container = container;

        setTitle(AppConfig.APP_TITLE);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1420, 860);
        setMinimumSize(new Dimension(1200, 740));
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        getContentPane().setBackground(GamingTheme.BG_APP);

        headerPanel = new HeaderPanel(container.getNotificationObserver(), this::refreshAllScreens);
        headerPanel.setCurrentUser(loggedInUser);
        headerPanel.updateNotificationCount();

        sidebarPanel = new SidebarPanel(this::navigateTo, this::handleLogout);

        cardLayout = new CardLayout();
        contentContainer = new JPanel(cardLayout);
        contentContainer.setBackground(GamingTheme.BG_APP);

        toastBannerLabel = new JLabel(" ");
        toastBannerLabel.setFont(GamingTheme.FONT_BODY_BOLD);
        toastBannerLabel.setForeground(GamingTheme.SUCCESS);
        toastBannerLabel.setOpaque(true);
        toastBannerLabel.setBackground(new Color(16, 185, 129, 28));
        toastBannerLabel.setBorder(BorderFactory.createEmptyBorder(6, 20, 6, 20));
        toastBannerLabel.setVisible(false);

        registerAllScreens();

        // Connect NotificationObserver listener to UI header & toast banner safely on EDT
        container.getNotificationObserver().setUiListener(msg -> SwingUtilities.invokeLater(() -> {
            headerPanel.updateNotificationCount();
            toastBannerLabel.setText(msg);
            toastBannerLabel.setVisible(true);
        }));

        JPanel rightArea = new JPanel(new BorderLayout());
        rightArea.setBackground(GamingTheme.BG_APP);

        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.add(headerPanel, BorderLayout.NORTH);
        topWrapper.add(toastBannerLabel, BorderLayout.SOUTH);

        rightArea.add(topWrapper, BorderLayout.NORTH);
        rightArea.add(contentContainer, BorderLayout.CENTER);

        add(sidebarPanel, BorderLayout.WEST);
        add(rightArea, BorderLayout.CENTER);

        navigateTo("DASHBOARD");
    }

    private void registerAllScreens() {
        DashboardPanel dashboardPanel = new DashboardPanel(container.getDashboardService());
        ProductPanel productPanel = new ProductPanel(
                container.getProductService(), container.getCategoryService(), container.getInventoryService());
        CategoryPanel categoryPanel = new CategoryPanel(container.getCategoryService());
        CustomerPanel customerPanel = new CustomerPanel(container.getCustomerService());
        OrderListPanel orderListPanel = new OrderListPanel(container.getOrderService(), this::openPaymentForOrder);
        CreateOrderPanel createOrderPanel = new CreateOrderPanel(
                container.getCustomerService(),
                container.getProductService(),
                container.getOrderService(),
                this::openPaymentForOrder,
                () -> navigateTo("ORDERS")
        );
        DiscountPanel discountPanel = new DiscountPanel(container.getOrderService());
        paymentPanel = new PaymentPanel(
                container.getOrderService(),
                container.getPaymentService(),
                container.getBillService(),
                this::openInvoiceById
        );
        invoicePanel = new InvoicePanel(container.getBillService(), container.getPdfInvoiceExporter());
        InventoryPanel inventoryPanel = new InventoryPanel(
                container.getProductService(), container.getInventoryService(), container.getAppConfig());
        SettingsPanel settingsPanel = new SettingsPanel(container, this::refreshAllScreens);

        addScreen("DASHBOARD", "Dashboard Tổng Quan", dashboardPanel);
        addScreen("PRODUCTS", "Quản Lý Sản Phẩm", productPanel);
        addScreen("CATEGORIES", "Quản Lý Danh Mục", categoryPanel);
        addScreen("CUSTOMERS", "Quản Lý Khách Hàng", customerPanel);
        addScreen("ORDERS", "Quản Lý Đơn Hàng", orderListPanel);
        addScreen("CREATE_ORDER", "Tạo Đơn Hàng Mới", createOrderPanel);
        addScreen("DISCOUNTS", "Chính Sách Giảm Giá", discountPanel);
        addScreen("PAYMENTS", "Thanh Toán Mô Phỏng", paymentPanel);
        addScreen("INVOICES", "Quản Lý Hóa Đơn", invoicePanel);
        addScreen("INVENTORY", "Quản Lý Tồn Kho", inventoryPanel);
        addScreen("SETTINGS", "Cài Đặt & Tài Khoản", settingsPanel);
    }

    private void addScreen(String key, String title, JPanel panel) {
        contentContainer.add(panel, key);
        screenTitles.put(key, title);
        if (panel instanceof RefreshablePanel refreshable) {
            screens.put(key, refreshable);
        }
    }

    public void navigateTo(String key) {
        this.currentKey = key;
        sidebarPanel.setActive(key);
        headerPanel.setPageTitle(screenTitles.getOrDefault(key, key));
        cardLayout.show(contentContainer, key);
        RefreshablePanel panel = screens.get(key);
        if (panel != null) {
            panel.refreshData();
        }
    }

    private void openPaymentForOrder(String orderId) {
        navigateTo("PAYMENTS");
        paymentPanel.selectOrderById(orderId);
    }

    private void openInvoiceById(String billId) {
        navigateTo("INVOICES");
        invoicePanel.selectBillById(billId);
    }

    private void refreshAllScreens() {
        RefreshablePanel current = screens.get(currentKey);
        if (current != null) {
            current.refreshData();
        }
        headerPanel.updateNotificationCount();
    }

    private void handleLogout() {
        if (!GamingTheme.showConfirm(this, "Bạn có muốn đăng xuất khỏi hệ thống?")) {
            return;
        }
        container.getNotificationObserver().setUiListener(null);
        container.getAuthService().logout();
        LoginFrame loginFrame = new LoginFrame(container);
        loginFrame.setVisible(true);
        dispose();
    }
}
