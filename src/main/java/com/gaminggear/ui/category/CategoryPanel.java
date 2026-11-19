package com.gaminggear.ui.category;

import com.gaminggear.model.Category;
import com.gaminggear.service.CategoryService;
import com.gaminggear.ui.components.RefreshablePanel;
import com.gaminggear.ui.theme.GamingTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen 4: Category Management (Keyboard, Mouse, Monitor, Headset, Speaker, Webcam, Controller, Microphone, Mouse Pad).
 */
public class CategoryPanel extends JPanel implements RefreshablePanel {

    private final CategoryService categoryService;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField nameField;
    private final JTextField descField;
    private final JCheckBox activeCheck;

    private List<Category> categories = new ArrayList<>();
    private String editingId = null;

    public CategoryPanel(CategoryService categoryService) {
        this.categoryService = categoryService;

        setLayout(new BorderLayout(16, 16));
        setBackground(GamingTheme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Left Table
        JPanel tableCard = GamingTheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 10));
        tableCard.add(GamingTheme.createSectionTitle("Danh sách danh mục Gaming Gear"), BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"ID", "Tên danh mục", "Mô tả", "Số sản phẩm", "Trạng thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateSelected();
            }
        });
        tableCard.add(GamingTheme.wrapTable(table), BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        // Right Form
        JPanel formCard = GamingTheme.createCardPanel();
        formCard.setPreferredSize(new Dimension(340, 0));
        formCard.setLayout(new BorderLayout(0, 12));
        formCard.add(GamingTheme.createSectionTitle("Thông tin danh mục"), BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.setOpaque(false);

        nameField = GamingTheme.createTextField(20);
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        descField = GamingTheme.createTextField(20);
        descField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        activeCheck = new JCheckBox("Đang hoạt động", true);
        activeCheck.setOpaque(false);
        activeCheck.setForeground(GamingTheme.TEXT_PRIMARY);
        activeCheck.setFont(GamingTheme.FONT_BODY);

        fields.add(GamingTheme.createFieldLabel("Tên danh mục *"));
        fields.add(Box.createVerticalStrut(6));
        fields.add(nameField);
        fields.add(Box.createVerticalStrut(14));
        fields.add(GamingTheme.createFieldLabel("Mô tả"));
        fields.add(Box.createVerticalStrut(6));
        fields.add(descField);
        fields.add(Box.createVerticalStrut(14));
        fields.add(activeCheck);

        JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
        buttons.setOpaque(false);

        JButton addBtn = GamingTheme.createSuccessButton("Thêm mới");
        addBtn.addActionListener(e -> createCategory());
        JButton updateBtn = GamingTheme.createPrimaryButton("Cập nhật");
        updateBtn.addActionListener(e -> updateCategory());
        JButton deleteBtn = GamingTheme.createDangerButton("Xóa");
        deleteBtn.addActionListener(e -> deleteCategory());
        JButton clearBtn = GamingTheme.createSecondaryButton("Làm sạch form");
        clearBtn.addActionListener(e -> clearForm());

        buttons.add(addBtn);
        buttons.add(updateBtn);
        buttons.add(deleteBtn);
        buttons.add(clearBtn);

        formCard.add(fields, BorderLayout.CENTER);
        formCard.add(buttons, BorderLayout.SOUTH);
        add(formCard, BorderLayout.EAST);

        refreshData();
    }

    @Override
    public void refreshData() {
        categories = categoryService.getAllCategories();
        tableModel.setRowCount(0);
        for (Category c : categories) {
            int count = categoryService.countProductsInCategory(c.getId());
            tableModel.addRow(new Object[]{
                    c.getId(),
                    c.getName(),
                    c.getDescription(),
                    count,
                    c.isActive() ? "Hoạt động" : "Tạm ẩn"
            });
        }
    }

    private void populateSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= categories.size()) {
            return;
        }
        Category c = categories.get(row);
        editingId = c.getId();
        nameField.setText(c.getName());
        descField.setText(c.getDescription());
        activeCheck.setSelected(c.isActive());
    }

    private void createCategory() {
        try {
            categoryService.createCategory(nameField.getText(), descField.getText(), activeCheck.isSelected());
            clearForm();
            refreshData();
            GamingTheme.showSuccess(this, "Đã thêm danh mục mới.");
        } catch (Exception ex) {
            GamingTheme.showError(this, ex.getMessage());
        }
    }

    private void updateCategory() {
        if (editingId == null) {
            GamingTheme.showError(this, "Vui lòng chọn danh mục cần cập nhật từ bảng.");
            return;
        }
        try {
            categoryService.updateCategory(editingId, nameField.getText(), descField.getText(), activeCheck.isSelected());
            refreshData();
            GamingTheme.showSuccess(this, "Đã cập nhật danh mục.");
        } catch (Exception ex) {
            GamingTheme.showError(this, ex.getMessage());
        }
    }

    private void deleteCategory() {
        if (editingId == null) {
            GamingTheme.showError(this, "Vui lòng chọn danh mục cần xóa.");
            return;
        }
        if (!GamingTheme.showConfirm(this, "Xác nhận xóa danh mục đang chọn?")) {
            return;
        }
        try {
            categoryService.deleteCategory(editingId);
            clearForm();
            refreshData();
            GamingTheme.showSuccess(this, "Đã xóa danh mục.");
        } catch (Exception ex) {
            GamingTheme.showError(this, ex.getMessage());
        }
    }

    private void clearForm() {
        editingId = null;
        nameField.setText("");
        descField.setText("");
        activeCheck.setSelected(true);
        table.clearSelection();
    }
}
