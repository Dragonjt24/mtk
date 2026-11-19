package com.gaminggear.ui.discount;

import com.gaminggear.patterns.factory.DiscountFactory;
import com.gaminggear.patterns.factory.DiscountType;
import com.gaminggear.patterns.strategy.DiscountStrategy;
import com.gaminggear.service.OrderService;
import com.gaminggear.ui.components.RefreshablePanel;
import com.gaminggear.ui.theme.GamingTheme;
import com.gaminggear.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Screen 8: Discount Policies overview and live Factory + Strategy interactive calculator.
 */
public class DiscountPanel extends JPanel implements RefreshablePanel {

    private final OrderService orderService;
    private final DefaultTableModel policyTableModel;
    private final JTextField amountField;
    private final JComboBox<DiscountType> strategyCombo;
    private final JLabel simResultLabel;

    public DiscountPanel(OrderService orderService) {
        this.orderService = orderService;

        setLayout(new BorderLayout(16, 16));
        setBackground(GamingTheme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Top: Policy Cards Table
        JPanel topCard = GamingTheme.createCardPanel();
        topCard.setLayout(new BorderLayout(0, 10));
        topCard.add(GamingTheme.createSectionTitle("Chính sách giảm giá hiện hành (Factory Pattern + Strategy Pattern)"), BorderLayout.NORTH);

        policyTableModel = new DefaultTableModel(
                new String[]{"Mã chính sách", "Tên hiển thị", "Tỷ lệ giảm", "Class Strategy", "Điều kiện áp dụng"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable policyTable = new JTable(policyTableModel);
        topCard.add(GamingTheme.wrapTable(policyTable), BorderLayout.CENTER);

        JLabel ruleNote = new JLabel("Quy tắc nghiệp vụ: Không tự động cộng dồn nhiều chính sách giảm giá trên cùng một đơn hàng. Chính sách VIP kiểm tra hạng thành viên ở tầng OrderService.");
        ruleNote.setFont(GamingTheme.FONT_BODY);
        ruleNote.setForeground(GamingTheme.WARNING);
        topCard.add(ruleNote, BorderLayout.SOUTH);
        add(topCard, BorderLayout.CENTER);

        // Bottom: Live Simulator
        JPanel simCard = GamingTheme.createCardPanel();
        simCard.setLayout(new BorderLayout(0, 12));
        simCard.add(GamingTheme.createSectionTitle("Mô phỏng tính toán chiến lược giảm giá"), BorderLayout.NORTH);

        JPanel simForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        simForm.setOpaque(false);
        amountField = GamingTheme.createTextField(14);
        amountField.setText("1000000");
        strategyCombo = new JComboBox<>(DiscountType.values());
        JButton calcBtn = GamingTheme.createNeonButton("Tính toán qua Factory + Strategy");
        calcBtn.addActionListener(e -> runSimulation());

        simForm.add(GamingTheme.createFieldLabel("Tổng tiền gốc (VNĐ):"));
        simForm.add(amountField);
        simForm.add(GamingTheme.createFieldLabel("Chọn chiến lược:"));
        simForm.add(strategyCombo);
        simForm.add(calcBtn);

        simResultLabel = new JLabel("Nhập số tiền và nhấn nút để kiểm tra thuật toán giảm giá.");
        simResultLabel.setFont(GamingTheme.FONT_SUBTITLE);
        simResultLabel.setForeground(GamingTheme.ACCENT_BLUE);

        simCard.add(simForm, BorderLayout.CENTER);
        simCard.add(simResultLabel, BorderLayout.SOUTH);
        add(simCard, BorderLayout.SOUTH);

        refreshData();
    }

    @Override
    public void refreshData() {
        policyTableModel.setRowCount(0);
        for (DiscountType type : DiscountType.values()) {
            DiscountStrategy strategy = DiscountFactory.createStrategy(type);
            String rule = switch (type) {
                case NO_DISCOUNT -> "Áp dụng cho tất cả đơn hàng";
                case VIP -> "Chỉ áp dụng cho khách hàng hạng VIP (kiểm tra tại OrderService)";
                case SEASONAL -> "Áp dụng cho sự kiện khuyến mãi theo mùa (không cộng dồn)";
            };
            policyTableModel.addRow(new Object[]{
                    type.name(),
                    type.getDisplayName(),
                    CurrencyUtil.formatPercent(strategy.getDiscountRate()),
                    strategy.getClass().getSimpleName(),
                    rule
            });
        }
        runSimulation();
    }

    private void runSimulation() {
        try {
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            DiscountType type = (DiscountType) strategyCombo.getSelectedItem();
            OrderService.PricingResult res = orderService.previewPricing(amount, type);
            simResultLabel.setForeground(GamingTheme.SUCCESS);
            simResultLabel.setText(String.format("Giá gốc: %s | Giảm (%s): -%s | Tổng thanh toán: %s",
                    CurrencyUtil.formatVnd(res.originalTotal()),
                    CurrencyUtil.formatPercent(res.discountRate()),
                    CurrencyUtil.formatVnd(res.discountAmount()),
                    CurrencyUtil.formatVnd(res.finalTotal())));
        } catch (NumberFormatException nfe) {
            simResultLabel.setForeground(GamingTheme.DANGER);
            simResultLabel.setText("Số tiền nhập vào không hợp lệ.");
        } catch (Exception ex) {
            simResultLabel.setForeground(GamingTheme.DANGER);
            simResultLabel.setText(ex.getMessage());
        }
    }
}
