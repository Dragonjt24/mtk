package com.gaminggear.ui.order;

import com.gaminggear.model.Customer;
import com.gaminggear.model.Order;
import com.gaminggear.model.Product;
import com.gaminggear.model.enums.ProductStatus;
import com.gaminggear.patterns.factory.DiscountType;
import com.gaminggear.service.CustomerService;
import com.gaminggear.service.OrderService;
import com.gaminggear.service.ProductService;
import com.gaminggear.ui.components.RefreshablePanel;
import com.gaminggear.ui.theme.GamingTheme;
import com.gaminggear.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Screen 7: Interactive Order Builder integrating DiscountFactory & DiscountStrategy live calculation.
 */
public class CreateOrderPanel extends JPanel implements RefreshablePanel {

    private final CustomerService customerService;
    private final ProductService productService;
    private final OrderService orderService;
    private final Consumer<String> onOrderCreatedForPayment;
    private final Runnable onOrderCreatedToList;

    private final JComboBox<Customer> customerCombo;
    private final JTextField productSearchField;
    private final DefaultTableModel catalogModel;
    private final JTable catalogTable;
    private final JSpinner qtySpinner;

    private final DefaultTableModel cartModel;
    private final JTable cartTable;
    private final JComboBox<DiscountType> discountCombo;

    private final JLabel strategyDescLabel;
    private final JLabel originalTotalLabel;
    private final JLabel discountRateLabel;
    private final JLabel discountAmountLabel;
    private final JLabel finalTotalLabel;

    private List<Product> availableProducts = new ArrayList<>();
    private final Map<String, CartEntry> cart = new LinkedHashMap<>();

    private static class CartEntry {
        final Product product;
        int quantity;

        CartEntry(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        BigDecimal getLineTotal() {
            return product.getPrice().multiply(BigDecimal.valueOf(quantity));
        }
    }

    public CreateOrderPanel(CustomerService customerService,
                            ProductService productService,
                            OrderService orderService,
                            Consumer<String> onOrderCreatedForPayment,
                            Runnable onOrderCreatedToList) {
        this.customerService = customerService;
        this.productService = productService;
        this.orderService = orderService;
        this.onOrderCreatedForPayment = onOrderCreatedForPayment;
        this.onOrderCreatedToList = onOrderCreatedToList;

        setLayout(new GridLayout(1, 2, 16, 0));
        setBackground(GamingTheme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // LEFT: Customer + Product Catalog
        JPanel leftCard = GamingTheme.createCardPanel();
        leftCard.setLayout(new BorderLayout(0, 12));

        JPanel leftTop = new JPanel();
        leftTop.setLayout(new BoxLayout(leftTop, BoxLayout.Y_AXIS));
        leftTop.setOpaque(false);

        JPanel custRow = new JPanel(new BorderLayout(8, 0));
        custRow.setOpaque(false);
        custRow.add(GamingTheme.createFieldLabel("Chọn khách hàng:"), BorderLayout.WEST);
        customerCombo = new JComboBox<>();
        custRow.add(customerCombo, BorderLayout.CENTER);

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        productSearchField = GamingTheme.createTextField(16);
        JButton searchBtn = GamingTheme.createPrimaryButton("Tìm SP");
        searchBtn.addActionListener(e -> loadCatalog());
        searchRow.add(GamingTheme.createFieldLabel("Tìm sản phẩm:"), BorderLayout.WEST);
        searchRow.add(productSearchField, BorderLayout.CENTER);
        searchRow.add(searchBtn, BorderLayout.EAST);

        leftTop.add(GamingTheme.createSectionTitle("1. Chọn khách hàng & sản phẩm"));
        leftTop.add(Box.createVerticalStrut(10));
        leftTop.add(custRow);
        leftTop.add(Box.createVerticalStrut(10));
        leftTop.add(searchRow);
        leftCard.add(leftTop, BorderLayout.NORTH);

        catalogModel = new DefaultTableModel(new String[]{"Mã SP", "Tên sản phẩm", "Giá bán", "Tồn kho"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        catalogTable = new JTable(catalogModel);
        catalogTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        leftCard.add(GamingTheme.wrapTable(catalogTable), BorderLayout.CENTER);

        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        addRow.setOpaque(false);
        qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        qtySpinner.setPreferredSize(new Dimension(75, 34));
        JButton addCartBtn = GamingTheme.createSuccessButton("+ Thêm vào giỏ hàng");
        addCartBtn.addActionListener(e -> addSelectedToCart());
        addRow.add(GamingTheme.createFieldLabel("Số lượng:"));
        addRow.add(qtySpinner);
        addRow.add(addCartBtn);
        leftCard.add(addRow, BorderLayout.SOUTH);

        // RIGHT: Cart + Discount Strategy + Checkout Summary
        JPanel rightCard = GamingTheme.createCardPanel();
        rightCard.setLayout(new BorderLayout(0, 12));

        rightCard.add(GamingTheme.createSectionTitle("2. Giỏ hàng & Áp dụng chiến lược giảm giá"), BorderLayout.NORTH);

        cartModel = new DefaultTableModel(new String[]{"Mã SP", "Sản phẩm", "Đơn giá", "SL", "Thành tiền"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cartTable = new JTable(cartModel);
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel cartCenter = new JPanel(new BorderLayout(0, 8));
        cartCenter.setOpaque(false);
        cartCenter.add(GamingTheme.wrapTable(cartTable), BorderLayout.CENTER);

        JPanel cartControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        cartControls.setOpaque(false);
        JButton incBtn = GamingTheme.createSecondaryButton("+ Tăng SL");
        incBtn.addActionListener(e -> adjustCartQty(1));
        JButton decBtn = GamingTheme.createSecondaryButton("- Giảm SL");
        decBtn.addActionListener(e -> adjustCartQty(-1));
        JButton removeBtn = GamingTheme.createDangerButton("Xóa khỏi giỏ");
        removeBtn.addActionListener(e -> removeSelectedFromCart());
        JButton clearCartBtn = GamingTheme.createSecondaryButton("Xóa hết giỏ");
        clearCartBtn.addActionListener(e -> {
            cart.clear();
            refreshCartAndPricing();
        });
        cartControls.add(incBtn);
        cartControls.add(decBtn);
        cartControls.add(removeBtn);
        cartControls.add(clearCartBtn);
        cartCenter.add(cartControls, BorderLayout.SOUTH);
        rightCard.add(cartCenter, BorderLayout.CENTER);

        // Pricing & Checkout Footer
        JPanel checkoutBox = new JPanel();
        checkoutBox.setLayout(new BoxLayout(checkoutBox, BoxLayout.Y_AXIS));
        checkoutBox.setBackground(GamingTheme.BG_ELEVATED);
        checkoutBox.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel discountRow = new JPanel(new BorderLayout(10, 0));
        discountRow.setOpaque(false);
        discountRow.add(GamingTheme.createFieldLabel("Chính sách giảm giá:"), BorderLayout.WEST);
        discountCombo = new JComboBox<>(DiscountType.values());
        discountCombo.addActionListener(e -> updatePricingSummary());
        discountRow.add(discountCombo, BorderLayout.CENTER);

        strategyDescLabel = new JLabel("Chiến lược: Không áp dụng giảm giá (0%)");
        strategyDescLabel.setFont(GamingTheme.FONT_SMALL);
        strategyDescLabel.setForeground(GamingTheme.ACCENT_PURPLE);

        originalTotalLabel = new JLabel("0 VNĐ");
        originalTotalLabel.setFont(GamingTheme.FONT_BODY_BOLD);
        originalTotalLabel.setForeground(GamingTheme.TEXT_PRIMARY);

        discountRateLabel = new JLabel("0%");
        discountRateLabel.setFont(GamingTheme.FONT_BODY_BOLD);
        discountRateLabel.setForeground(GamingTheme.ACCENT_BLUE);

        discountAmountLabel = new JLabel("-0 VNĐ");
        discountAmountLabel.setFont(GamingTheme.FONT_BODY_BOLD);
        discountAmountLabel.setForeground(GamingTheme.WARNING);

        finalTotalLabel = new JLabel("0 VNĐ");
        finalTotalLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        finalTotalLabel.setForeground(GamingTheme.SUCCESS);

        JPanel summaryGrid = new JPanel(new GridLayout(4, 2, 8, 6));
        summaryGrid.setOpaque(false);
        summaryGrid.add(GamingTheme.createFieldLabel("Giá gốc:"));
        summaryGrid.add(originalTotalLabel);
        summaryGrid.add(GamingTheme.createFieldLabel("Tỷ lệ giảm:"));
        summaryGrid.add(discountRateLabel);
        summaryGrid.add(GamingTheme.createFieldLabel("Số tiền được giảm:"));
        summaryGrid.add(discountAmountLabel);
        summaryGrid.add(GamingTheme.createFieldLabel("TỔNG THANH TOÁN:"));
        summaryGrid.add(finalTotalLabel);

        JPanel submitRow = new JPanel(new GridLayout(1, 2, 10, 0));
        submitRow.setOpaque(false);
        JButton createPendingBtn = GamingTheme.createPrimaryButton("Tạo đơn hàng (PENDING)");
        createPendingBtn.addActionListener(e -> submitOrder(false));
        JButton createAndPayBtn = GamingTheme.createSuccessButton("Tạo đơn & Thanh toán ngay");
        createAndPayBtn.addActionListener(e -> submitOrder(true));
        submitRow.add(createPendingBtn);
        submitRow.add(createAndPayBtn);

        checkoutBox.add(discountRow);
        checkoutBox.add(Box.createVerticalStrut(6));
        checkoutBox.add(strategyDescLabel);
        checkoutBox.add(Box.createVerticalStrut(10));
        checkoutBox.add(summaryGrid);
        checkoutBox.add(Box.createVerticalStrut(12));
        checkoutBox.add(submitRow);

        rightCard.add(checkoutBox, BorderLayout.SOUTH);

        add(leftCard);
        add(rightCard);

        refreshData();
    }

    @Override
    public void refreshData() {
        Customer currentSelected = (Customer) customerCombo.getSelectedItem();
        customerCombo.removeAllItems();
        for (Customer c : customerService.getAllCustomers()) {
            customerCombo.addItem(c);
        }
        if (currentSelected != null) {
            for (int i = 0; i < customerCombo.getItemCount(); i++) {
                if (customerCombo.getItemAt(i).getId().equals(currentSelected.getId())) {
                    customerCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        loadCatalog();
        refreshCartAndPricing();
    }

    private void loadCatalog() {
        String q = productSearchField.getText();
        availableProducts = productService.searchAndFilter(q, "", "", null, null, ProductService.SortOption.NAME_ASC)
                .stream()
                .filter(p -> p.getStatus() != ProductStatus.INACTIVE)
                .toList();
        catalogModel.setRowCount(0);
        for (Product p : availableProducts) {
            catalogModel.addRow(new Object[]{
                    p.getId(),
                    p.getName(),
                    CurrencyUtil.formatVnd(p.getPrice()),
                    p.getStockQuantity()
            });
        }
    }

    private void addSelectedToCart() {
        int row = catalogTable.getSelectedRow();
        if (row < 0 || row >= availableProducts.size()) {
            GamingTheme.showError(this, "Vui lòng chọn sản phẩm để thêm vào giỏ hàng.");
            return;
        }
        Product p = availableProducts.get(row);
        int qty = (int) qtySpinner.getValue();
        int existingQty = cart.containsKey(p.getId()) ? cart.get(p.getId()).quantity : 0;
        if (existingQty + qty > p.getStockQuantity()) {
            GamingTheme.showError(this, "Số lượng vượt quá tồn kho hiện có (" + p.getStockQuantity() + ").");
            return;
        }
        if (cart.containsKey(p.getId())) {
            cart.get(p.getId()).quantity += qty;
        } else {
            cart.put(p.getId(), new CartEntry(p, qty));
        }
        refreshCartAndPricing();
    }

    private void adjustCartQty(int delta) {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            GamingTheme.showError(this, "Vui lòng chọn dòng sản phẩm trong giỏ.");
            return;
        }
        String productId = (String) cartModel.getValueAt(row, 0);
        CartEntry entry = cart.get(productId);
        if (entry == null) {
            return;
        }
        int nextQty = entry.quantity + delta;
        if (nextQty <= 0) {
            cart.remove(productId);
        } else if (nextQty > entry.product.getStockQuantity()) {
            GamingTheme.showError(this, "Số lượng không được vượt quá tồn kho (" + entry.product.getStockQuantity() + ").");
            return;
        } else {
            entry.quantity = nextQty;
        }
        refreshCartAndPricing();
        if (row < cartModel.getRowCount()) {
            cartTable.setRowSelectionInterval(row, row);
        }
    }

    private void removeSelectedFromCart() {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            GamingTheme.showError(this, "Vui lòng chọn sản phẩm cần xóa khỏi giỏ.");
            return;
        }
        String productId = (String) cartModel.getValueAt(row, 0);
        cart.remove(productId);
        refreshCartAndPricing();
    }

    private void refreshCartAndPricing() {
        cartModel.setRowCount(0);
        for (CartEntry entry : cart.values()) {
            cartModel.addRow(new Object[]{
                    entry.product.getId(),
                    entry.product.getName(),
                    CurrencyUtil.formatVnd(entry.product.getPrice()),
                    entry.quantity,
                    CurrencyUtil.formatVnd(entry.getLineTotal())
            });
        }
        updatePricingSummary();
    }

    private void updatePricingSummary() {
        BigDecimal originalTotal = cart.values().stream()
                .map(CartEntry::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DiscountType selectedType = (DiscountType) discountCombo.getSelectedItem();
        if (selectedType == null) {
            selectedType = DiscountType.NO_DISCOUNT;
        }
        OrderService.PricingResult pricing = orderService.previewPricing(originalTotal, selectedType);
        strategyDescLabel.setText("Chiến lược áp dụng: " + pricing.strategyDescription());
        originalTotalLabel.setText(CurrencyUtil.formatVnd(pricing.originalTotal()));
        discountRateLabel.setText(CurrencyUtil.formatPercent(pricing.discountRate()));
        discountAmountLabel.setText("-" + CurrencyUtil.formatVnd(pricing.discountAmount()));
        finalTotalLabel.setText(CurrencyUtil.formatVnd(pricing.finalTotal()));
    }

    private void submitOrder(boolean goToPayment) {
        Customer customer = (Customer) customerCombo.getSelectedItem();
        if (customer == null) {
            GamingTheme.showError(this, "Vui lòng chọn khách hàng.");
            return;
        }
        List<OrderService.OrderLineRequest> lines = new ArrayList<>();
        for (CartEntry entry : cart.values()) {
            lines.add(new OrderService.OrderLineRequest(entry.product.getId(), entry.quantity));
        }
        DiscountType discountType = (DiscountType) discountCombo.getSelectedItem();
        try {
            Order created = orderService.createOrder(customer.getId(), lines, discountType);
            cart.clear();
            refreshCartAndPricing();
            if (goToPayment && onOrderCreatedForPayment != null) {
                GamingTheme.showSuccess(this, "Đã tạo đơn hàng " + created.getId() + ". Chuyển sang màn hình thanh toán.");
                onOrderCreatedForPayment.accept(created.getId());
            } else {
                GamingTheme.showSuccess(this, "Đã tạo đơn hàng " + created.getId() + " ở trạng thái PENDING.");
                if (onOrderCreatedToList != null) {
                    onOrderCreatedToList.run();
                }
            }
        } catch (Exception ex) {
            GamingTheme.showError(this, ex.getMessage());
        }
    }
}
