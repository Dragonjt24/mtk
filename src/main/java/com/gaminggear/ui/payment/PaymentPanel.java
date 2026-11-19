package com.gaminggear.ui.payment;

import com.gaminggear.model.Bill;
import com.gaminggear.model.Order;
import com.gaminggear.model.Payment;
import com.gaminggear.model.enums.OrderStatus;
import com.gaminggear.model.enums.PaymentMethod;
import com.gaminggear.model.enums.PaymentStatus;
import com.gaminggear.service.BillService;
import com.gaminggear.service.OrderService;
import com.gaminggear.service.PaymentService;
import com.gaminggear.ui.components.RefreshablePanel;
import com.gaminggear.ui.theme.GamingTheme;
import com.gaminggear.util.CurrencyUtil;
import com.gaminggear.util.DateTimeUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Screen 9: Simulated Payment screen (COD, Mock Success, Mock Failure) driving Observer Pattern.
 */
public class PaymentPanel extends JPanel implements RefreshablePanel {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final BillService billService;
    private final Consumer<String> onOpenInvoice;

    private final DefaultTableModel pendingOrderModel;
    private final JTable pendingOrderTable;
    private final JComboBox<PaymentMethod> methodCombo;
    private final JLabel selectedOrderSummary;
    private final JButton openLastBillBtn;

    private final DefaultTableModel paymentHistoryModel;

    private List<Order> pendingOrders = new ArrayList<>();
    private String lastCreatedBillId = null;

    public PaymentPanel(OrderService orderService,
                        PaymentService paymentService,
                        BillService billService,
                        Consumer<String> onOpenInvoice) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.billService = billService;
        this.onOpenInvoice = onOpenInvoice;

        setLayout(new BorderLayout(14, 14));
        setBackground(GamingTheme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Top Split: Pending Orders + Payment Action Box
        JPanel topSplit = new JPanel(new GridLayout(1, 2, 16, 0));
        topSplit.setOpaque(false);

        JPanel pendingCard = GamingTheme.createCardPanel();
        pendingCard.setLayout(new BorderLayout(0, 8));
        pendingCard.add(GamingTheme.createSectionTitle("Đơn hàng chờ thanh toán (PENDING)"), BorderLayout.NORTH);
        pendingOrderModel = new DefaultTableModel(
                new String[]{"Mã đơn", "Khách hàng", "Giá gốc", "Giảm giá", "Thanh toán"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        pendingOrderTable = new JTable(pendingOrderModel);
        pendingOrderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pendingOrderTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectedSummary();
            }
        });
        pendingCard.add(GamingTheme.wrapTable(pendingOrderTable), BorderLayout.CENTER);

        JPanel actionCard = GamingTheme.createCardPanel();
        actionCard.setLayout(new BoxLayout(actionCard, BoxLayout.Y_AXIS));
        JLabel actTitle = GamingTheme.createSectionTitle("Thực hiện thanh toán mô phỏng (Observer Pattern)");
        actTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        selectedOrderSummary = new JLabel("Chọn một đơn hàng PENDING ở bảng bên trái.");
        selectedOrderSummary.setFont(GamingTheme.FONT_BODY_BOLD);
        selectedOrderSummary.setForeground(GamingTheme.ACCENT_BLUE);
        selectedOrderSummary.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel methodLbl = GamingTheme.createFieldLabel("Chọn phương thức / kịch bản thanh toán:");
        methodLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        methodCombo = new JComboBox<>(PaymentMethod.values());
        methodCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        methodCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea observerInfo = new JTextArea(
                "Khi thanh toán thành công (COD / Mock Success):\n"
                        + "• Đơn hàng chuyển sang PAID\n"
                        + "• PaymentEventPublisher phát sự kiện PaymentEvent\n"
                        + "• BillObserver tự động tạo hóa đơn\n"
                        + "• InventoryObserver tự động trừ tồn kho\n"
                        + "• NotificationObserver gửi thông báo thành công\n"
                        + "Khi Mock Failure: KHÔNG tạo hóa đơn, KHÔNG trừ tồn kho.");
        observerInfo.setEditable(false);
        observerInfo.setBackground(GamingTheme.BG_ELEVATED);
        observerInfo.setForeground(GamingTheme.TEXT_SECONDARY);
        observerInfo.setFont(GamingTheme.FONT_SMALL);
        observerInfo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        observerInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton payBtn = GamingTheme.createSuccessButton("Thanh toán ngay");
        payBtn.addActionListener(e -> executePayment());

        openLastBillBtn = GamingTheme.createNeonButton("Mở hóa đơn vừa tạo");
        openLastBillBtn.setEnabled(false);
        openLastBillBtn.addActionListener(e -> {
            if (lastCreatedBillId != null && onOpenInvoice != null) {
                onOpenInvoice.accept(lastCreatedBillId);
            }
        });
        btnRow.add(payBtn);
        btnRow.add(openLastBillBtn);

        actionCard.add(actTitle);
        actionCard.add(Box.createVerticalStrut(12));
        actionCard.add(selectedOrderSummary);
        actionCard.add(Box.createVerticalStrut(14));
        actionCard.add(methodLbl);
        actionCard.add(Box.createVerticalStrut(6));
        actionCard.add(methodCombo);
        actionCard.add(Box.createVerticalStrut(14));
        actionCard.add(observerInfo);
        actionCard.add(Box.createVerticalStrut(14));
        actionCard.add(btnRow);

        topSplit.add(pendingCard);
        topSplit.add(actionCard);
        add(topSplit, BorderLayout.CENTER);

        // Bottom: Payment History
        JPanel historyCard = GamingTheme.createCardPanel();
        historyCard.setPreferredSize(new Dimension(0, 240));
        historyCard.setLayout(new BorderLayout(0, 8));
        historyCard.add(GamingTheme.createSectionTitle("Lịch sử giao dịch thanh toán"), BorderLayout.NORTH);

        paymentHistoryModel = new DefaultTableModel(
                new String[]{"Mã GD", "Mã đơn", "Phương thức", "Số tiền", "Kết quả", "Thời gian", "Thông điệp"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable historyTable = new JTable(paymentHistoryModel);
        historyCard.add(GamingTheme.wrapTable(historyTable), BorderLayout.CENTER);
        add(historyCard, BorderLayout.SOUTH);

        refreshData();
    }

    @Override
    public void refreshData() {
        pendingOrders = orderService.getOrdersByStatus(OrderStatus.PENDING);
        pendingOrderModel.setRowCount(0);
        for (Order o : pendingOrders) {
            pendingOrderModel.addRow(new Object[]{
                    o.getId(),
                    o.getCustomerName(),
                    CurrencyUtil.formatVnd(o.getOriginalTotal()),
                    CurrencyUtil.formatVnd(o.getDiscountAmount()),
                    CurrencyUtil.formatVnd(o.getFinalTotal())
            });
        }
        updateSelectedSummary();

        paymentHistoryModel.setRowCount(0);
        for (Payment p : paymentService.getAllPayments()) {
            paymentHistoryModel.addRow(new Object[]{
                    p.getId(),
                    p.getOrderId(),
                    p.getMethod().name(),
                    CurrencyUtil.formatVnd(p.getAmount()),
                    p.getStatus().name(),
                    DateTimeUtil.format(p.getPaidAt()),
                    p.getMessage()
            });
        }
    }

    public void selectOrderById(String orderId) {
        refreshData();
        for (int i = 0; i < pendingOrders.size(); i++) {
            if (pendingOrders.get(i).getId().equals(orderId)) {
                pendingOrderTable.setRowSelectionInterval(i, i);
                break;
            }
        }
    }

    private void updateSelectedSummary() {
        int row = pendingOrderTable.getSelectedRow();
        if (row < 0 || row >= pendingOrders.size()) {
            selectedOrderSummary.setText("Chọn một đơn hàng PENDING ở bảng bên trái.");
            return;
        }
        Order o = pendingOrders.get(row);
        selectedOrderSummary.setText(String.format("Đơn %s | KH: %s | Cần thu: %s",
                o.getId(), o.getCustomerName(), CurrencyUtil.formatVnd(o.getFinalTotal())));
    }

    private void executePayment() {
        int row = pendingOrderTable.getSelectedRow();
        if (row < 0 || row >= pendingOrders.size()) {
            GamingTheme.showError(this, "Vui lòng chọn một đơn hàng PENDING để thanh toán.");
            return;
        }
        Order selectedOrder = pendingOrders.get(row);
        PaymentMethod method = (PaymentMethod) methodCombo.getSelectedItem();
        try {
            Payment payment = paymentService.processPayment(selectedOrder.getId(), method);
            refreshData();
            if (payment.getStatus() == PaymentStatus.FAILED) {
                GamingTheme.showError(this, "Thanh toán THẤT BẠI!\n" + payment.getMessage()
                        + "\nHóa đơn KHÔNG được tạo và tồn kho KHÔNG bị trừ.");
            } else {
                Optional<Bill> billOpt = billService.findByOrderId(selectedOrder.getId());
                if (billOpt.isPresent()) {
                    lastCreatedBillId = billOpt.get().getId();
                    openLastBillBtn.setEnabled(true);
                }
                GamingTheme.showSuccess(this, "Thanh toán THÀNH CÔNG!\n"
                        + "• Đơn hàng " + selectedOrder.getId() + " -> PAID\n"
                        + "• Hóa đơn: " + (lastCreatedBillId != null ? lastCreatedBillId : "Đã tạo") + "\n"
                        + "• Đã cập nhật tồn kho & gửi thông báo qua Observer Pattern.");
            }
        } catch (Exception ex) {
            GamingTheme.showError(this, ex.getMessage());
        }
    }
}
