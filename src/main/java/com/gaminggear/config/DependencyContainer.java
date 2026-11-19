package com.gaminggear.config;

import com.gaminggear.patterns.observer.BillObserver;
import com.gaminggear.patterns.observer.InventoryObserver;
import com.gaminggear.patterns.observer.NotificationObserver;
import com.gaminggear.patterns.observer.PaymentEventPublisher;
import com.gaminggear.patterns.strategy.DiscountContext;
import com.gaminggear.repository.BillRepository;
import com.gaminggear.repository.CategoryRepository;
import com.gaminggear.repository.CustomerRepository;
import com.gaminggear.repository.InventoryLogRepository;
import com.gaminggear.repository.OrderRepository;
import com.gaminggear.repository.PaymentRepository;
import com.gaminggear.repository.ProductRepository;
import com.gaminggear.repository.UserRepository;
import com.gaminggear.service.AuthService;
import com.gaminggear.service.BillService;
import com.gaminggear.service.CategoryService;
import com.gaminggear.service.CustomerService;
import com.gaminggear.service.DashboardService;
import com.gaminggear.service.InventoryService;
import com.gaminggear.service.OrderService;
import com.gaminggear.service.PaymentService;
import com.gaminggear.service.PdfInvoiceExporter;
import com.gaminggear.service.ProductService;
import com.gaminggear.storage.DataInitializer;
import com.gaminggear.storage.InMemoryDataStore;
import com.gaminggear.storage.JSONStorageService;
import com.gaminggear.storage.StorageSync;

/**
 * Manual Dependency Injection container wiring repositories, services, and design patterns.
 */
public class DependencyContainer {

    private final AppConfig appConfig;
    private final InMemoryDataStore dataStore;
    private final JSONStorageService jsonStorageService;
    private final StorageSync storageSync;

    private final AuthService authService;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final CustomerService customerService;
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final BillService billService;
    private final PaymentService paymentService;
    private final DashboardService dashboardService;
    private final PdfInvoiceExporter pdfInvoiceExporter;

    private final DiscountContext discountContext;
    private final PaymentEventPublisher paymentEventPublisher;
    private final BillObserver billObserver;
    private final InventoryObserver inventoryObserver;
    private final NotificationObserver notificationObserver;

    public DependencyContainer() {
        this(new AppConfig(), true);
    }

    public DependencyContainer(AppConfig appConfig, boolean loadFromDisk) {
        this.appConfig = appConfig;
        this.dataStore = new InMemoryDataStore();
        this.jsonStorageService = new JSONStorageService(appConfig);
        this.storageSync = new StorageSync(appConfig, dataStore, jsonStorageService);

        if (!loadFromDisk || !jsonStorageService.load(dataStore)) {
            DataInitializer.initialize(dataStore);
            if (loadFromDisk && appConfig.isAutoSaveEnabled()) {
                jsonStorageService.save(dataStore);
            }
        }

        UserRepository userRepository = new UserRepository(dataStore);
        CategoryRepository categoryRepository = new CategoryRepository(dataStore);
        ProductRepository productRepository = new ProductRepository(dataStore);
        CustomerRepository customerRepository = new CustomerRepository(dataStore);
        OrderRepository orderRepository = new OrderRepository(dataStore);
        PaymentRepository paymentRepository = new PaymentRepository(dataStore);
        BillRepository billRepository = new BillRepository(dataStore);
        InventoryLogRepository inventoryLogRepository = new InventoryLogRepository(dataStore);

        this.discountContext = new DiscountContext();
        this.paymentEventPublisher = new PaymentEventPublisher();

        this.authService = new AuthService(userRepository, storageSync);
        this.categoryService = new CategoryService(categoryRepository, productRepository, storageSync);
        this.productService = new ProductService(productRepository, categoryRepository, storageSync);
        this.customerService = new CustomerService(customerRepository, orderRepository, storageSync);
        this.inventoryService = new InventoryService(productRepository, inventoryLogRepository, appConfig, storageSync);
        this.orderService = new OrderService(orderRepository, customerRepository, productRepository, discountContext, storageSync);
        this.billService = new BillService(billRepository, customerRepository, storageSync);

        // Register Observers with Publisher
        this.billObserver = new BillObserver(billService);
        this.inventoryObserver = new InventoryObserver(inventoryService);
        this.notificationObserver = new NotificationObserver();

        this.paymentEventPublisher.registerObserver(billObserver);
        this.paymentEventPublisher.registerObserver(inventoryObserver);
        this.paymentEventPublisher.registerObserver(notificationObserver);

        this.paymentService = new PaymentService(
                orderRepository, paymentRepository, inventoryService, customerService, paymentEventPublisher, storageSync
        );
        this.dashboardService = new DashboardService(productRepository, categoryRepository, orderRepository, appConfig);
        this.pdfInvoiceExporter = new PdfInvoiceExporter();
    }

    public void resetToSampleData() {
        DataInitializer.initialize(dataStore);
        storageSync.persistIfEnabled();
    }

    public void saveNow() {
        storageSync.forceSave();
    }

    public boolean reloadFromJson() {
        return jsonStorageService.load(dataStore);
    }

    public AppConfig getAppConfig() {
        return appConfig;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public CategoryService getCategoryService() {
        return categoryService;
    }

    public ProductService getProductService() {
        return productService;
    }

    public CustomerService getCustomerService() {
        return customerService;
    }

    public InventoryService getInventoryService() {
        return inventoryService;
    }

    public OrderService getOrderService() {
        return orderService;
    }

    public BillService getBillService() {
        return billService;
    }

    public PaymentService getPaymentService() {
        return paymentService;
    }

    public DashboardService getDashboardService() {
        return dashboardService;
    }

    public PdfInvoiceExporter getPdfInvoiceExporter() {
        return pdfInvoiceExporter;
    }

    public DiscountContext getDiscountContext() {
        return discountContext;
    }

    public PaymentEventPublisher getPaymentEventPublisher() {
        return paymentEventPublisher;
    }

    public BillObserver getBillObserver() {
        return billObserver;
    }

    public InventoryObserver getInventoryObserver() {
        return inventoryObserver;
    }

    public NotificationObserver getNotificationObserver() {
        return notificationObserver;
    }
}
