package com.gaminggear.ui.order;

import com.gaminggear.model.Order;
import com.gaminggear.model.OrderItem;
import com.gaminggear.model.enums.OrderStatus;
import com.gaminggear.patterns.factory.DiscountType;
import com.gaminggear.service.OrderService;
import com.gaminggear.ui.components.RefreshablePanel;
import com.gaminggear.ui.theme.GamingTheme;
import com.gaminggear.util.CurrencyUtil;
import com.gaminggear.util.DateTimeUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Screen 6: Order List, Order Item snapshot detail viewer, discount switcher (PENDING), cancel, and pay trigger.
 */
public class OrderListPanel extends JPanel implements RefreshablePanel {

    private final OrderService orderService;
    private final Consumer<String> onNavigateToPayment;

    private final JComboBox<String> statusFilterCombo;
    private final JTextField searchField;
    private final DefaultTableModel orderTableModel;
    private final JTable orderTable;
    private final DefaultTableModel itemTableModel;
    private final JLabel detailSummaryLabel;

    private List<Order> displayedOrders = new ArrayList<>();

    public OrderListPanel(OrderService orderService, Consumer<String> onNavigateToPayment) {
        this.orderService = orderService;
        this.onNavigateToPayment = onNavigateToPayment;

        setLayout(new BorderLayout(14, 14));
        setBackground(GamingTheme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Top Toolbar
        JPanel topCard = GamingTheme.createCardPanel();
        topCard.setLayout(new BorderLayout(10, 0));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterPanel.setOpaque(false);
        searchField = GamingTheme.createTextField(16);
        statusFilterCombo = new JComboBox<>(new String[]{"Tất cả", "PENDING", "PAID", "CANCELLED"});
        JButton filterBtn = GamingTheme.createPrimaryButton("Lọc đơn hàng");
        filterBtn.addActionListener(e -> loadOrders());
        filterPanel.add(GamingTheme.createFieldLabel("Mã đơn / Khách hàng:"));
        filterPanel.add(searchField);
        filterPanel.add(GamingTheme.createFieldLabel("Trạng thái:"));
        filterPanel.add(statusFilterCombo);
        filterPanel.add(filterBtn);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);
        JButton changeDiscountBtn = GamingTheme.createNeonButton("Đổi giảm giá");
        changeDiscountBtn.addActionListener(e -> changeSelectedDiscount());
        JButton payBtn = GamingTheme.createSuccessButton("Thanh toán đơn");
        payBtn.addActionListener(e -> goToPayment());
        JButton cancelBtn = GamingTheme.createDangerButton("Hủy đơn hàng");
        cancelBtn.addActionListener(e -> cancelSelectedOrder());

        actionPanel.add(changeDiscountBtn);
        actionPanel.add(payBtn);
        actionPanel.add(cancelBtn);

        topCard.add(filterPanel, BorderLayout.WEST);
        topCard.add(actionPanel, BorderLayout.EAST);
        add(topCard, BorderLayout.NORTH);

        // Split: Orders Table + Order Detail Snapshot
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.55);
        split.setDividerSize(8);
        split.setBorder(null);
        split.setBackground(GamingTheme.BG_APP);

        orderTableModel = new DefaultTableModel(
                new String[]{"Mã đơn", "Khách hàng", "Chính sách giảm", "Giá gốc", "Tiền giảm", "Thanh toán", "Ngày tạo", "Trạng thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        orderTable = new JTable(orderTableModel);
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        orderTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedOrderDetails();
            }
        });

        JPanel ordersCard = GamingTheme.createCardPanel();
        ordersCard.setLayout(new BorderLayout(0, 8));
        ordersCard.add(GamingTheme.createSectionTitle("Danh sách đơn hàng"), BorderLayout.NORTH);
        ordersCard.add(GamingTheme.wrapTable(orderTable), BorderLayout.CENTER);

        JPanel detailCard = GamingTheme.createCardPanel();
        detailCard.setLayout(new BorderLayout(0, 8));

        JPanel detailHeader = new JPanel(new BorderLayout());
        detailHeader.setOpaque(false);
        detailHeader.add(GamingTheme.createSectionTitle("Chi tiết sản phẩm trong đơn hàng (Snapshot đơn giá tại thời điểm mua)"), BorderLayout.WEST);
        detailSummaryLabel = new JLabel("Chọn đơn hàng để xem chi tiết.");
        detailSummaryLabel.setFont(GamingTheme.FONT_BODY_BOLD);
        detailSummaryLabel.setForeground(GamingTheme.ACCENT_BLUE);
        detailHeader.add(detailSummaryLabel, BorderLayout.EAST);

        itemTableModel = new DefaultTableModel(
                new String[]{"Mã SP", "Tên sản phẩm", "Số lượng", "Đơn giá lúc mua", "Thành tiền"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable itemTable = new JTable(itemTableModel);

        detailCard.add(detailHeader, BorderLayout.NORTH);
        detailCard.add(GamingTheme.wrapTable(itemTable), BorderLayout.CENTER);

        split.setTopComponent(ordersCard);
        split.setBottomComponent(detailCard);
        add(split, BorderLayout.CENTER);

        refreshData();
    }

    @Override
    public void refreshData() {
        loadOrders();
    }

    private void loadOrders() {
        String selectedStatus = (String) statusFilterCombo.getSelectedItem();
        OrderStatus filterStatus = null;
        if (selectedStatus != null && !"Tất cả".equals(selectedStatus)) {
            filterStatus = OrderStatus.valueOf(selectedStatus);
        }
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        displayedOrders = orderService.getOrdersByStatus(filterStatus).stream()
                .filter(o -> q.isEmpty()
                        || o.getId().toLowerCase().contains(q)
                        || o.getCustomerName().toLowerCase().contains(q))
                .toList();

        orderTableModel.setRowCount(0);
        for (Order o : displayedOrders) {
            orderTableModel.addRow(new Object[]{
                    o.getId(),
                    o.getCustomerName(),
                    o.getDiscountType().getDisplayName(),
                    CurrencyUtil.formatVnd(o.getOriginalTotal()),
                    CurrencyUtil.formatVnd(o.getDiscountAmount()),
                    CurrencyUtil.formatVnd(o.getFinalTotal()),
                    DateTimeUtil.format(o.getCreatedAt()),
                    o.getStatus().name()
            });
        }
        itemTableModel.setRowCount(0);
        detailSummaryLabel.setText("Chọn đơn hàng để xem chi tiết.");
    }

    private void showSelectedOrderDetails() {
        itemTableModel.setRowCount(0);
        int row = orderTable.getSelectedRow();
        if (row < 0 || row >= displayedOrders.size()) {
            return;
        }
        Order order = displayedOrders.get(row);
        for (OrderItem item : order.getItems()) {
            itemTableModel.addRow(new Object[]{
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    CurrencyUtil.formatVnd(item.getUnitPrice()),
                    CurrencyUtil.formatVnd(item.getLineTotal())
            });
        }
        detailSummaryLabel.setText(String.format("Đơn %s | %s | Thanh toán: %s",
                order.getId(), order.getStatus().name(), CurrencyUtil.formatVnd(order.getFinalTotal())));
    }

    private Order getSelectedOrder() {
        int row = orderTable.getSelectedRow();
        if (row < 0 || row >= displayedOrders.size()) {
            GamingTheme.showError(this, "Vui lòng chọn một đơn hàng.");
            return null;
        }
        return displayedOrders.get(row);
    }

    private void changeSelectedDiscount() {
        Order order = getSelectedOrder();
        if (order == null) {
            return;
        }
        DiscountType selected = (DiscountType) JOptionPane.showInputDialog(
                this,
                "Chọn chính sách giảm giá mới cho đơn hàng " + order.getId() + ":",
                "Cập nhật giảm giá (Factory + Strategy)",
                JOptionPane.QUESTION_MESSAGE,
                null,
                DiscountType.values(),
                order.getDiscountType()
        );
        if (selected == null) {
            return;
        }
        try {
            orderService.changeOrderDiscount(order.getId(), selected);
            refreshData();
            GamingTheme.showSuccess(this, "Đã cập nhật chính sách giảm giá cho đơn " + order.getId() + ".");
        } catch (Exception ex) {
            GamingTheme.showError(this, ex.getMessage());
        }
    }

    private void cancelSelectedOrder() {
        Order order = getSelectedOrder();
        if (order == null) {
            return;
        }
        if (!GamingTheme.showConfirm(this, "Bạn có chắc chắn muốn hủy đơn hàng " + order.getId() + "?")) {
            return;
        }
        try {
            orderService.cancelOrder(order.getId());
            refreshData();
            GamingTheme.showSuccess(this, "Đã hủy đơn hàng " + order.getId() + ".");
        } catch (Exception ex) {
            GamingTheme.showError(this, ex.getMessage());
        }
    }

    private void goToPayment() {
        Order order = getSelectedOrder();
        if (order == null) {
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            GamingTheme.showError(this, "Chỉ đơn hàng trạng thái PENDING mới có thể chuyển sang thanh toán.");
            return;
        }
        if (onNavigateToPayment != null) {
            onNavigateToPayment.accept(order.getId());
        }
    }
}
