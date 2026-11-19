package com.gaminggear.ui.settings;

import com.gaminggear.config.AppConfig;
import com.gaminggear.config.DependencyContainer;
import com.gaminggear.model.User;
import com.gaminggear.model.enums.UserRole;
import com.gaminggear.ui.components.RefreshablePanel;
import com.gaminggear.ui.theme.GamingTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen 12: Basic Staff/Account Management, System Threshold Settings, and JSON Storage Controls.
 */
public class SettingsPanel extends JPanel implements RefreshablePanel {

    private final DependencyContainer container;
    private final Runnable onGlobalRefresh;

    private final DefaultTableModel userTableModel;
    private final JTable userTable;
    private final JTextField thresholdField;
    private final JCheckBox autoSaveCheck;
    private final JLabel storagePathLabel;

    private List<User> users = new ArrayList<>();

    public SettingsPanel(DependencyContainer container, Runnable onGlobalRefresh) {
        this.container = container;
        this.onGlobalRefresh = onGlobalRefresh;

        setLayout(new GridLayout(1, 2, 16, 0));
        setBackground(GamingTheme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Left: Staff & Account Management
        JPanel userCard = GamingTheme.createCardPanel();
        userCard.setLayout(new BorderLayout(0, 12));
        userCard.add(GamingTheme.createSectionTitle("Quản lý nhân viên & tài khoản đăng nhập"), BorderLayout.NORTH);

        userTableModel = new DefaultTableModel(
                new String[]{"ID", "Username", "Họ tên", "Vai trò", "Trạng thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new JTable(userTableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userCard.add(GamingTheme.wrapTable(userTable), BorderLayout.CENTER);

        JPanel userBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        userBtns.setOpaque(false);
        JButton addUserBtn = GamingTheme.createSuccessButton("+ Thêm tài khoản");
        addUserBtn.addActionListener(e -> openAddUserDialog());
        JButton toggleActiveBtn = GamingTheme.createPrimaryButton("Khóa / Mở khóa");
        toggleActiveBtn.addActionListener(e -> toggleSelectedUserActive());
        JButton resetPassBtn = GamingTheme.createNeonButton("Đổi mật khẩu");
        resetPassBtn.addActionListener(e -> changeSelectedUserPassword());

        userBtns.add(addUserBtn);
        userBtns.add(toggleActiveBtn);
        userBtns.add(resetPassBtn);
        userCard.add(userBtns, BorderLayout.SOUTH);

        // Right: Storage & App Config
        JPanel configCard = GamingTheme.createCardPanel();
        configCard.setLayout(new BoxLayout(configCard, BoxLayout.Y_AXIS));

        JLabel cfgTitle = GamingTheme.createSectionTitle("Cấu hình hệ thống & Lưu trữ JSON cục bộ");
        cfgTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        AppConfig cfg = container.getAppConfig();
        storagePathLabel = new JLabel("Tệp JSON: " + cfg.getStoragePath().toAbsolutePath());
        storagePathLabel.setFont(GamingTheme.FONT_SMALL);
        storagePathLabel.setForeground(GamingTheme.ACCENT_BLUE);
        storagePathLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel thresholdRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        thresholdRow.setOpaque(false);
        thresholdRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        thresholdField = GamingTheme.createTextField(8);
        thresholdField.setText(String.valueOf(cfg.getLowStockThreshold()));
        JButton applyCfgBtn = GamingTheme.createPrimaryButton("Áp dụng cấu hình");
        applyCfgBtn.addActionListener(e -> applyConfig());

        thresholdRow.add(GamingTheme.createFieldLabel("Ngưỡng cảnh báo sắp hết hàng:"));
        thresholdRow.add(thresholdField);
        thresholdRow.add(applyCfgBtn);

        autoSaveCheck = new JCheckBox("Tự động lưu thay đổi vào tệp JSON", cfg.isAutoSaveEnabled());
        autoSaveCheck.setOpaque(false);
        autoSaveCheck.setForeground(GamingTheme.TEXT_PRIMARY);
        autoSaveCheck.setFont(GamingTheme.FONT_BODY);
        autoSaveCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoSaveCheck.addActionListener(e -> cfg.setAutoSaveEnabled(autoSaveCheck.isSelected()));

        JPanel jsonBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        jsonBtns.setOpaque(false);
        jsonBtns.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton saveNowBtn = GamingTheme.createSuccessButton("Lưu JSON ngay");
        saveNowBtn.addActionListener(e -> {
            try {
                container.saveNow();
                GamingTheme.showSuccess(this, "Đã lưu toàn bộ dữ liệu bộ nhớ ra tệp JSON.");
            } catch (Exception ex) {
                GamingTheme.showError(this, ex.getMessage());
            }
        });

        JButton reloadBtn = GamingTheme.createSecondaryButton("Tải lại từ JSON");
        reloadBtn.addActionListener(e -> {
            boolean ok = container.reloadFromJson();
            if (ok) {
                if (onGlobalRefresh != null) {
                    onGlobalRefresh.run();
                }
                GamingTheme.showSuccess(this, "Đã tải lại dữ liệu từ tệp JSON.");
            } else {
                GamingTheme.showError(this, "Không tìm thấy tệp JSON hợp lệ để tải.");
            }
        });

        JButton resetSampleBtn = GamingTheme.createDangerButton("Khôi phục dữ liệu mẫu");
        resetSampleBtn.addActionListener(e -> {
            if (!GamingTheme.showConfirm(this, "Khôi phục toàn bộ dữ liệu mẫu ban đầu? Các thay đổi chưa sao lưu sẽ bị thay thế.")) {
                return;
            }
            container.resetToSampleData();
            if (onGlobalRefresh != null) {
                onGlobalRefresh.run();
            }
            GamingTheme.showSuccess(this, "Đã khôi phục dữ liệu mẫu thành công.");
        });

        jsonBtns.add(saveNowBtn);
        jsonBtns.add(reloadBtn);
        jsonBtns.add(resetSampleBtn);

        JTextArea archNote = new JTextArea(
                "Kiến trúc lưu trữ:\n"
                        + "• Toàn bộ dữ liệu vận hành trong InMemoryDataStore (LinkedHashMap, ArrayList).\n"
                        + "• KHÔNG sử dụng bất kỳ cơ sở dữ liệu (SQL/NoSQL/JDBC/ORM) nào.\n"
                        + "• JSONStorageService chỉ đóng vai trò đọc/ghi tệp cục bộ để giữ dữ liệu giữa các phiên.");
        archNote.setEditable(false);
        archNote.setLineWrap(true);
        archNote.setWrapStyleWord(true);
        archNote.setBackground(GamingTheme.BG_ELEVATED);
        archNote.setForeground(GamingTheme.TEXT_SECONDARY);
        archNote.setFont(GamingTheme.FONT_SMALL);
        archNote.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        archNote.setAlignmentX(Component.LEFT_ALIGNMENT);

        configCard.add(cfgTitle);
        configCard.add(Box.createVerticalStrut(12));
        configCard.add(storagePathLabel);
        configCard.add(Box.createVerticalStrut(14));
        configCard.add(thresholdRow);
        configCard.add(Box.createVerticalStrut(10));
        configCard.add(autoSaveCheck);
        configCard.add(Box.createVerticalStrut(14));
        configCard.add(jsonBtns);
        configCard.add(Box.createVerticalStrut(18));
        configCard.add(archNote);

        add(userCard);
        add(configCard);

        refreshData();
    }

    @Override
    public void refreshData() {
        users = container.getAuthService().getAllUsers();
        userTableModel.setRowCount(0);
        for (User u : users) {
            userTableModel.addRow(new Object[]{
                    u.getId(),
                    u.getUsername(),
                    u.getFullName(),
                    u.getRole().name(),
                    u.isActive() ? "Hoạt động" : "Đã khóa"
            });
        }
    }

    private void openAddUserDialog() {
        JTextField usernameField = GamingTheme.createTextField(16);
        JPasswordField passwordField = new JPasswordField(16);
        JTextField fullNameField = GamingTheme.createTextField(16);
        JComboBox<UserRole> roleCombo = new JComboBox<>(UserRole.values());

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.add(new JLabel("Tên đăng nhập *:"));
        form.add(usernameField);
        form.add(new JLabel("Mật khẩu (>= 6 ký tự) *:"));
        form.add(passwordField);
        form.add(new JLabel("Họ và tên *:"));
        form.add(fullNameField);
        form.add(new JLabel("Vai trò:"));
        form.add(roleCombo);

        int res = JOptionPane.showConfirmDialog(this, form, "Thêm tài khoản nhân viên",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            container.getAuthService().createUser(
                    usernameField.getText(),
                    new String(passwordField.getPassword()),
                    fullNameField.getText(),
                    (UserRole) roleCombo.getSelectedItem()
            );
            refreshData();
            GamingTheme.showSuccess(this, "Đã tạo tài khoản mới.");
        } catch (Exception ex) {
            GamingTheme.showError(this, ex.getMessage());
        }
    }

    private void toggleSelectedUserActive() {
        int row = userTable.getSelectedRow();
        if (row < 0 || row >= users.size()) {
            GamingTheme.showError(this, "Vui lòng chọn tài khoản.");
            return;
        }
        User u = users.get(row);
        try {
            container.getAuthService().updateUserStatus(u.getId(), !u.isActive(), u.getRole(), u.getFullName());
            refreshData();
            GamingTheme.showSuccess(this, "Đã cập nhật trạng thái tài khoản " + u.getUsername() + ".");
        } catch (Exception ex) {
            GamingTheme.showError(this, ex.getMessage());
        }
    }

    private void changeSelectedUserPassword() {
        int row = userTable.getSelectedRow();
        if (row < 0 || row >= users.size()) {
            GamingTheme.showError(this, "Vui lòng chọn tài khoản cần đổi mật khẩu.");
            return;
        }
        User u = users.get(row);
        String newPass = JOptionPane.showInputDialog(this,
                "Nhập mật khẩu mới cho '" + u.getUsername() + "' (>= 6 ký tự):");
        if (newPass == null) {
            return;
        }
        try {
            container.getAuthService().changePassword(u.getId(), newPass);
            GamingTheme.showSuccess(this, "Đã đổi mật khẩu cho " + u.getUsername() + ".");
        } catch (Exception ex) {
            GamingTheme.showError(this, ex.getMessage());
        }
    }

    private void applyConfig() {
        try {
            int threshold = Integer.parseInt(thresholdField.getText().trim());
            container.getAppConfig().setLowStockThreshold(threshold);
            if (onGlobalRefresh != null) {
                onGlobalRefresh.run();
            }
            GamingTheme.showSuccess(this, "Đã cập nhật ngưỡng cảnh báo tồn kho: " + threshold + ".");
        } catch (NumberFormatException nfe) {
            GamingTheme.showError(this, "Ngưỡng cảnh báo phải là số nguyên không âm.");
        } catch (Exception ex) {
            GamingTheme.showError(this, ex.getMessage());
        }
    }
}
