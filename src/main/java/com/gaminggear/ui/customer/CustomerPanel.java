package com.gaminggear.ui.customer;

import com.gaminggear.model.Customer;
import com.gaminggear.model.Order;
import com.gaminggear.model.enums.MembershipTier;
import com.gaminggear.service.CustomerService;
import com.gaminggear.ui.components.RefreshablePanel;
import com.gaminggear.ui.theme.GamingTheme;
import com.gaminggear.util.CurrencyUtil;
import com.gaminggear.util.DateTimeUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen 5: Customer Management with CRUD, search, membership tier, and purchase history viewer.
 */
public class CustomerPanel extends JPanel implements RefreshablePanel {

    private final CustomerService customerService;

    private final JTextField searchField;
    private final DefaultTableModel customerModel;
    private final JTable customerTable;
    private final DefaultTableModel historyModel;

    private List<Customer> currentCustomers = new ArrayList<>();

    public CustomerPanel(CustomerService customerService) {
        this.customerService = customerService;

        setLayout(new BorderLayout(14, 14));
        setBackground(GamingTheme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Top Bar
        JPanel topCard = GamingTheme.createCardPanel();
        topCard.setLayout(new BorderLayout(12, 0));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchBar.setOpaque(false);
        searchField = GamingTheme.createTextField(22);
        JButton searchBtn = GamingTheme.createPrimaryButton("Tìm kiếm");
        searchBtn.addActionListener(e -> searchCustomers());
        JButton resetBtn = GamingTheme.createSecondaryButton("Tất cả");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            searchCustomers();
        });
        searchBar.add(GamingTheme.createFieldLabel("Tìm tên / SĐT / Email:"));
        searchBar.add(searchField);
        searchBar.add(searchBtn);
        searchBar.add(resetBtn);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton addBtn = GamingTheme.createSuccessButton("+ Thêm khách hàng");
        addBtn.addActionListener(e -> openCustomerDialog(null));
        JButton editBtn = GamingTheme.createPrimaryButton("Chỉnh sửa");
        editBtn.addActionListener(e -> editSelected());
        JButton delBtn = GamingTheme.createDangerButton("Xóa");
        delBtn.addActionListener(e -> deleteSelected());
        actions.add(addBtn);
        actions.add(editBtn);
        actions.add(delBtn);

        topCard.add(searchBar, BorderLayout.WEST);
        topCard.add(actions, BorderLayout.EAST);
        add(topCard, BorderLayout.NORTH);

        // Center Split: Customer Table + Purchase History
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.58);
        splitPane.setDividerSize(8);
        splitPane.setBorder(null);
        splitPane.setBackground(GamingTheme.BG_APP);

        customerModel = new DefaultTableModel(
                new String[]{"ID", "Họ tên", "Số điện thoại", "Email", "Địa chỉ", "Hạng", "Tổng chi tiêu"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        customerTable = new JTable(customerModel);
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedHistory();
            }
        });

        JPanel customersCard = GamingTheme.createCardPanel();
        customersCard.setLayout(new BorderLayout(0, 8));
        customersCard.add(GamingTheme.createSectionTitle("Danh sách khách hàng"), BorderLayout.NORTH);
        customersCard.add(GamingTheme.wrapTable(customerTable), BorderLayout.CENTER);

        historyModel = new DefaultTableModel(
                new String[]{"Mã đơn", "Số SP", "Giảm giá", "Thành tiền", "Trạng thái", "Ngày đặt"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable historyTable = new JTable(historyModel);
        JPanel historyCard = GamingTheme.createCardPanel();
        historyCard.setLayout(new BorderLayout(0, 8));
        historyCard.add(GamingTheme.createSectionTitle("Lịch sử mua hàng của khách hàng đang chọn"), BorderLayout.NORTH);
        historyCard.add(GamingTheme.wrapTable(historyTable), BorderLayout.CENTER);

        splitPane.setTopComponent(customersCard);
        splitPane.setBottomComponent(historyCard);
        add(splitPane, BorderLayout.CENTER);

        refreshData();
    }

    @Override
    public void refreshData() {
        searchCustomers();
    }

    private void searchCustomers() {
        currentCustomers = customerService.searchCustomers(searchField.getText());
        customerModel.setRowCount(0);
        for (Customer c : currentCustomers) {
            customerModel.addRow(new Object[]{
                    c.getId(),
                    c.getFullName(),
                    c.getPhone(),
                    c.getEmail(),
                    c.getAddress(),
                    c.getMembershipTier().name(),
                    CurrencyUtil.formatVnd(c.getTotalSpent())
            });
        }
        historyModel.setRowCount(0);
    }

    private void loadSelectedHistory() {
        historyModel.setRowCount(0);
        int row = customerTable.getSelectedRow();
        if (row < 0 || row >= currentCustomers.size()) {
            return;
        }
        Customer selected = currentCustomers.get(row);
        List<Order> orders = customerService.getPurchaseHistory(selected.getId());
        for (Order o : orders) {
            historyModel.addRow(new Object[]{
                    o.getId(),
                    o.getItems().size(),
                    CurrencyUtil.formatVnd(o.getDiscountAmount()),
                    CurrencyUtil.formatVnd(o.getFinalTotal()),
                    o.getStatus().name(),
                    DateTimeUtil.format(o.getCreatedAt())
            });
        }
    }

    private void editSelected() {
        int row = customerTable.getSelectedRow();
        if (row < 0 || row >= currentCustomers.size()) {
            GamingTheme.showError(this, "Vui lòng chọn khách hàng cần chỉnh sửa.");
            return;
        }
        openCustomerDialog(currentCustomers.get(row));
    }

    private void deleteSelected() {
        int row = customerTable.getSelectedRow();
        if (row < 0 || row >= currentCustomers.size()) {
            GamingTheme.showError(this, "Vui lòng chọn khách hàng cần xóa.");
            return;
        }
        Customer c = currentCustomers.get(row);
        if (!GamingTheme.showConfirm(this, "Xác nhận xóa khách hàng '" + c.getFullName() + "'?")) {
            return;
        }
        try {
            customerService.deleteCustomer(c.getId());
            refreshData();
            GamingTheme.showSuccess(this, "Đã xóa khách hàng.");
        } catch (Exception ex) {
            GamingTheme.showError(this, ex.getMessage());
        }
    }

    private void openCustomerDialog(Customer existing) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                existing == null ? "Thêm khách hàng mới" : "Cập nhật khách hàng",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(460, 380);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 12));
        form.setBackground(GamingTheme.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JTextField nameField = GamingTheme.createTextField(20);
        JTextField phoneField = GamingTheme.createTextField(20);
        JTextField emailField = GamingTheme.createTextField(20);
        JTextField addressField = GamingTheme.createTextField(20);
        JComboBox<MembershipTier> tierCombo = new JComboBox<>(MembershipTier.values());

        if (existing != null) {
            nameField.setText(existing.getFullName());
            phoneField.setText(existing.getPhone());
            emailField.setText(existing.getEmail());
            addressField.setText(existing.getAddress());
            tierCombo.setSelectedItem(existing.getMembershipTier());
        }

        form.add(GamingTheme.createFieldLabel("Họ và tên *"));
        form.add(nameField);
        form.add(GamingTheme.createFieldLabel("Số điện thoại *"));
        form.add(phoneField);
        form.add(GamingTheme.createFieldLabel("Email *"));
        form.add(emailField);
        form.add(GamingTheme.createFieldLabel("Địa chỉ *"));
        form.add(addressField);
        form.add(GamingTheme.createFieldLabel("Hạng thành viên"));
        form.add(tierCombo);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btns.setBackground(GamingTheme.BG_CARD);
        JButton cancelBtn = GamingTheme.createSecondaryButton("Hủy");
        cancelBtn.addActionListener(e -> dialog.dispose());
        JButton saveBtn = GamingTheme.createPrimaryButton("Lưu khách hàng");
        saveBtn.addActionListener(e -> {
            try {
                MembershipTier tier = (MembershipTier) tierCombo.getSelectedItem();
                if (existing == null) {
                    customerService.createCustomer(
                            nameField.getText(), phoneField.getText(), emailField.getText(),
                            addressField.getText(), tier
                    );
                    GamingTheme.showSuccess(dialog, "Đã thêm khách hàng mới.");
                } else {
                    customerService.updateCustomer(
                            existing.getId(), nameField.getText(), phoneField.getText(),
                            emailField.getText(), addressField.getText(), tier
                    );
                    GamingTheme.showSuccess(dialog, "Đã cập nhật thông tin khách hàng.");
                }
                dialog.dispose();
                refreshData();
            } catch (Exception ex) {
                GamingTheme.showError(dialog, ex.getMessage());
            }
        });
        btns.add(cancelBtn);
        btns.add(saveBtn);

        dialog.setLayout(new BorderLayout());
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btns, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
