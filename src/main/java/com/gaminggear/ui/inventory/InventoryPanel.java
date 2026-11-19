package com.gaminggear.ui.inventory;

import com.gaminggear.config.AppConfig;
import com.gaminggear.model.InventoryLog;
import com.gaminggear.model.Product;
import com.gaminggear.service.InventoryService;
import com.gaminggear.service.ProductService;
import com.gaminggear.ui.components.RefreshablePanel;
import com.gaminggear.ui.theme.GamingTheme;
import com.gaminggear.util.DateTimeUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen 11: Inventory Management, low-stock alerts, stock-in/adjustment, and movement logs.
 */
public class InventoryPanel extends JPanel implements RefreshablePanel {

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final AppConfig appConfig;

    private final JComboBox<String> filterCombo;
    private final DefaultTableModel stockTableModel;
    private final JTable stockTable;
    private final DefaultTableModel logTableModel;

    private List<Product> displayedProducts = new ArrayList<>();

    public InventoryPanel(ProductService productService,
                          InventoryService inventoryService,
                          AppConfig appConfig) {
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.appConfig = appConfig;

        setLayout(new BorderLayout(14, 14));
        setBackground(GamingTheme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Top Bar
        JPanel topCard = GamingTheme.createCardPanel();
        topCard.setLayout(new BorderLayout(12, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        filterCombo = new JComboBox<>(new String[]{"Tất cả sản phẩm", "Sắp hết hàng", "Đã hết hàng"});
        filterCombo.addActionListener(e -> loadProducts());
        left.add(GamingTheme.createFieldLabel("Bộ lọc tồn kho:"));
        left.add(filterCombo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton stockInBtn = GamingTheme.createSuccessButton("+ Nhập thêm hàng");
        stockInBtn.addActionListener(e -> adjustSelected(true));
        JButton adjustBtn = GamingTheme.createPrimaryButton("Điều chỉnh số lượng");
        adjustBtn.addActionListener(e -> adjustSelected(false));
        right.add(stockInBtn);
        right.add(adjustBtn);

        topCard.add(left, BorderLayout.WEST);
        topCard.add(right, BorderLayout.EAST);
        add(topCard, BorderLayout.NORTH);

        // Split: Stock Levels + Movement Logs
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.55);
        split.setDividerSize(8);
        split.setBorder(null);
        split.setBackground(GamingTheme.BG_APP);

        stockTableModel = new DefaultTableModel(
                new String[]{"Mã SP", "Tên sản phẩm", "Thương hiệu", "Tồn kho", "Cảnh báo", "Trạng thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        stockTable = new JTable(stockTableModel);
        stockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel stockCard = GamingTheme.createCardPanel();
        stockCard.setLayout(new BorderLayout(0, 8));
        stockCard.add(GamingTheme.createSectionTitle("Tình trạng tồn kho sản phẩm"), BorderLayout.NORTH);
        stockCard.add(GamingTheme.wrapTable(stockTable), BorderLayout.CENTER);

        logTableModel = new DefaultTableModel(
                new String[]{"Mã Log", "Mã SP", "Sản phẩm", "Biến động", "Tồn trước", "Tồn sau", "Lý do", "Thời gian"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable logTable = new JTable(logTableModel);

        JPanel logCard = GamingTheme.createCardPanel();
        logCard.setLayout(new BorderLayout(0, 8));
        logCard.add(GamingTheme.createSectionTitle("Lịch sử nhập / xuất / điều chỉnh kho"), BorderLayout.NORTH);
        logCard.add(GamingTheme.wrapTable(logTable), BorderLayout.CENTER);

        split.setTopComponent(stockCard);
        split.setBottomComponent(logCard);
        add(split, BorderLayout.CENTER);

        refreshData();
    }

    @Override
    public void refreshData() {
        loadProducts();
        loadLogs();
    }

    private void loadProducts() {
        int threshold = appConfig.getLowStockThreshold();
        String filter = (String) filterCombo.getSelectedItem();
        List<Product> all = productService.getAllProducts();
        displayedProducts = all.stream().filter(p -> {
            if ("Sắp hết hàng".equals(filter)) {
                return p.getStockQuantity() <= threshold;
            }
            if ("Đã hết hàng".equals(filter)) {
                return p.getStockQuantity() == 0;
            }
            return true;
        }).toList();

        stockTableModel.setRowCount(0);
        for (Product p : displayedProducts) {
            String alert = p.getStockQuantity() == 0
                    ? "HẾT HÀNG"
                    : (p.getStockQuantity() <= threshold ? "SẮP HẾT (<= " + threshold + ")" : "Ổn định");
            stockTableModel.addRow(new Object[]{
                    p.getId(),
                    p.getName(),
                    p.getBrand(),
                    p.getStockQuantity(),
                    alert,
                    p.getStatus().getDisplayName()
            });
        }
    }

    private void loadLogs() {
        logTableModel.setRowCount(0);
        for (InventoryLog log : inventoryService.getAllLogs()) {
            String deltaStr = log.getChangeAmount() > 0 ? "+" + log.getChangeAmount() : String.valueOf(log.getChangeAmount());
            logTableModel.addRow(new Object[]{
                    log.getId(),
                    log.getProductId(),
                    log.getProductName(),
                    deltaStr,
                    log.getPreviousStock(),
                    log.getNewStock(),
                    log.getReason(),
                    DateTimeUtil.format(log.getCreatedAt())
            });
        }
    }

    private void adjustSelected(boolean stockInMode) {
        int row = stockTable.getSelectedRow();
        if (row < 0 || row >= displayedProducts.size()) {
            GamingTheme.showError(this, "Vui lòng chọn một sản phẩm.");
            return;
        }
        Product p = displayedProducts.get(row);
        String prompt = stockInMode
                ? "Nhập số lượng cộng thêm vào kho cho '" + p.getName() + "' (hiện có " + p.getStockQuantity() + "):"
                : "Nhập số lượng tồn kho mới cho '" + p.getName() + "' (hiện có " + p.getStockQuantity() + "):";

        JTextField qtyField = GamingTheme.createTextField(10);
        qtyField.setText(stockInMode ? "10" : String.valueOf(p.getStockQuantity()));
        JTextField reasonField = GamingTheme.createTextField(20);
        reasonField.setText(stockInMode ? "Nhập hàng bổ sung từ nhà cung cấp" : "Kiểm kê điều chỉnh kho");

        JPanel panel = new JPanel(new GridLayout(3, 1, 6, 8));
        panel.add(new JLabel(prompt));
        panel.add(qtyField);
        panel.add(reasonField);

        int res = JOptionPane.showConfirmDialog(this, panel,
                stockInMode ? "Nhập kho" : "Điều chỉnh tồn kho",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            int val = Integer.parseInt(qtyField.getText().trim());
            if (stockInMode) {
                if (val <= 0) {
                    throw new IllegalArgumentException("Số lượng nhập kho phải lớn hơn 0.");
                }
                inventoryService.adjustStock(p.getId(), val, reasonField.getText());
            } else {
                inventoryService.setStockQuantity(p.getId(), val, reasonField.getText());
            }
            refreshData();
            GamingTheme.showSuccess(this, "Đã cập nhật tồn kho thành công.");
        } catch (NumberFormatException nfe) {
            GamingTheme.showError(this, "Số lượng phải là số nguyên hợp lệ.");
        } catch (Exception ex) {
            GamingTheme.showError(this, ex.getMessage());
        }
    }
}
