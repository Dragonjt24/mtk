package com.gaminggear.ui.product;

import com.gaminggear.model.Category;
import com.gaminggear.model.Product;
import com.gaminggear.model.enums.ProductStatus;
import com.gaminggear.service.CategoryService;
import com.gaminggear.service.InventoryService;
import com.gaminggear.service.ProductService;
import com.gaminggear.ui.components.RefreshablePanel;
import com.gaminggear.ui.theme.GamingTheme;
import com.gaminggear.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Screen 3: Product Management with CRUD, batch delete, multi-criteria filtering, sorting, and stock update.
 */
public class ProductPanel extends JPanel implements RefreshablePanel {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final InventoryService inventoryService;

    private final JTextField searchField;
    private final JComboBox<CategoryItem> categoryFilterCombo;
    private final JComboBox<String> brandFilterCombo;
    private final JTextField minPriceField;
    private final JTextField maxPriceField;
    private final JComboBox<ProductService.SortOption> sortCombo;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextArea detailPreviewArea;

    private List<Product> currentProducts = new ArrayList<>();

    private record CategoryItem(String id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    public ProductPanel(ProductService productService,
                        CategoryService categoryService,
                        InventoryService inventoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.inventoryService = inventoryService;

        setLayout(new BorderLayout(14, 14));
        setBackground(GamingTheme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Top Filter & Action Toolbar
        JPanel topCard = GamingTheme.createCardPanel();
        topCard.setLayout(new BorderLayout(0, 12));

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filterRow.setOpaque(false);

        searchField = GamingTheme.createTextField(16);
        categoryFilterCombo = new JComboBox<>();
        brandFilterCombo = new JComboBox<>();
        minPriceField = GamingTheme.createTextField(7);
        maxPriceField = GamingTheme.createTextField(7);
        sortCombo = new JComboBox<>(ProductService.SortOption.values());

        JButton searchBtn = GamingTheme.createPrimaryButton("Lọc / Tìm");
        searchBtn.addActionListener(e -> applyFilters());
        JButton resetFilterBtn = GamingTheme.createSecondaryButton("Đặt lại");
        resetFilterBtn.addActionListener(e -> {
            searchField.setText("");
            categoryFilterCombo.setSelectedIndex(0);
            brandFilterCombo.setSelectedIndex(0);
            minPriceField.setText("");
            maxPriceField.setText("");
            sortCombo.setSelectedIndex(0);
            applyFilters();
        });

        filterRow.add(GamingTheme.createFieldLabel("Tìm tên/mã:"));
        filterRow.add(searchField);
        filterRow.add(GamingTheme.createFieldLabel("Danh mục:"));
        filterRow.add(categoryFilterCombo);
        filterRow.add(GamingTheme.createFieldLabel("Hãng:"));
        filterRow.add(brandFilterCombo);
        filterRow.add(GamingTheme.createFieldLabel("Giá từ:"));
        filterRow.add(minPriceField);
        filterRow.add(GamingTheme.createFieldLabel("đến:"));
        filterRow.add(maxPriceField);
        filterRow.add(GamingTheme.createFieldLabel("Sắp xếp:"));
        filterRow.add(sortCombo);
        filterRow.add(searchBtn);
        filterRow.add(resetFilterBtn);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionRow.setOpaque(false);

        JButton addBtn = GamingTheme.createSuccessButton("+ Thêm sản phẩm");
        addBtn.addActionListener(e -> openProductDialog(null));

        JButton editBtn = GamingTheme.createPrimaryButton("Chỉnh sửa");
        editBtn.addActionListener(e -> editSelectedProduct());

        JButton stockBtn = GamingTheme.createNeonButton("Cập nhật tồn kho");
        stockBtn.addActionListener(e -> quickUpdateStock());

        JButton deleteBtn = GamingTheme.createDangerButton("Xóa sản phẩm");
        deleteBtn.addActionListener(e -> deleteSingleSelected());

        JButton batchDeleteBtn = GamingTheme.createDangerButton("Xóa nhiều đã chọn");
        batchDeleteBtn.addActionListener(e -> deleteMultipleSelected());

        actionRow.add(addBtn);
        actionRow.add(editBtn);
        actionRow.add(stockBtn);
        actionRow.add(deleteBtn);
        actionRow.add(batchDeleteBtn);

        topCard.add(filterRow, BorderLayout.NORTH);
        topCard.add(actionRow, BorderLayout.SOUTH);
        add(topCard, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(
                new String[]{"ID", "Tên sản phẩm", "Danh mục", "Thương hiệu", "Giá bán", "Tồn kho", "Hình ảnh", "Trạng thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDetailPreview();
            }
        });

        JPanel centerSplit = new JPanel(new BorderLayout(0, 12));
        centerSplit.setOpaque(false);
        centerSplit.add(GamingTheme.wrapTable(table), BorderLayout.CENTER);

        detailPreviewArea = new JTextArea(3, 40);
        detailPreviewArea.setEditable(false);
        detailPreviewArea.setLineWrap(true);
        detailPreviewArea.setWrapStyleWord(true);
        detailPreviewArea.setBackground(GamingTheme.BG_CARD);
        detailPreviewArea.setForeground(GamingTheme.TEXT_SECONDARY);
        detailPreviewArea.setFont(GamingTheme.FONT_BODY);
        detailPreviewArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(GamingTheme.BORDER_SUBTLE), "Mô tả & Thông tin chi tiết sản phẩm"),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        centerSplit.add(detailPreviewArea, BorderLayout.SOUTH);

        add(centerSplit, BorderLayout.CENTER);
        refreshData();
    }

    @Override
    public void refreshData() {
        loadFilterDropdowns();
        applyFilters();
    }

    private void loadFilterDropdowns() {
        CategoryItem selectedCat = (CategoryItem) categoryFilterCombo.getSelectedItem();
        categoryFilterCombo.removeAllItems();
        categoryFilterCombo.addItem(new CategoryItem("", "Tất cả"));
        for (Category c : categoryService.getAllCategories()) {
            categoryFilterCombo.addItem(new CategoryItem(c.getId(), c.getName()));
        }
        if (selectedCat != null) {
            for (int i = 0; i < categoryFilterCombo.getItemCount(); i++) {
                if (categoryFilterCombo.getItemAt(i).id().equals(selectedCat.id())) {
                    categoryFilterCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        Object selectedBrand = brandFilterCombo.getSelectedItem();
        brandFilterCombo.removeAllItems();
        brandFilterCombo.addItem("Tất cả");
        for (String b : productService.getAllBrands()) {
            brandFilterCombo.addItem(b);
        }
        if (selectedBrand != null) {
            brandFilterCombo.setSelectedItem(selectedBrand);
        }
    }

    private void applyFilters() {
        try {
            String keyword = searchField.getText();
            CategoryItem cat = (CategoryItem) categoryFilterCombo.getSelectedItem();
            String catId = cat == null ? "" : cat.id();
            String brand = (String) brandFilterCombo.getSelectedItem();
            if ("Tất cả".equals(brand)) {
                brand = "";
            }
            BigDecimal minPrice = parseOptionalDecimal(minPriceField.getText(), "Giá tối thiểu");
            BigDecimal maxPrice = parseOptionalDecimal(maxPriceField.getText(), "Giá tối đa");
            ProductService.SortOption sort = (ProductService.SortOption) sortCombo.getSelectedItem();

            currentProducts = productService.searchAndFilter(keyword, catId, brand, minPrice, maxPrice, sort);
            renderTable();
        } catch (Exception ex) {
            GamingTheme.showError(this, ex.getMessage());
        }
    }

    private void renderTable() {
        Map<String, String> catNames = categoryService.getAllCategories().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        tableModel.setRowCount(0);
        for (Product p : currentProducts) {
            tableModel.addRow(new Object[]{
                    p.getId(),
                    p.getName(),
                    catNames.getOrDefault(p.getCategoryId(), p.getCategoryId()),
                    p.getBrand(),
                    CurrencyUtil.formatVnd(p.getPrice()),
                    p.getStockQuantity(),
                    p.getImagePath(),
                    p.getStatus().getDisplayName()
            });
        }
        updateDetailPreview();
    }

    private void updateDetailPreview() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentProducts.size()) {
            detailPreviewArea.setText("Chọn một sản phẩm trên bảng để xem mô tả chi tiết (giữ Ctrl/Shift để chọn nhiều sản phẩm khi xóa hàng loạt).");
            return;
        }
        Product p = currentProducts.get(row);
        detailPreviewArea.setText(String.format("[%s] %s | Hãng: %s | Giá: %s | Tồn kho: %d | Ảnh: %s%nMô tả: %s",
                p.getId(), p.getName(), p.getBrand(), CurrencyUtil.formatVnd(p.getPrice()),
                p.getStockQuantity(), p.getImagePath(), p.getDescription()));
    }

    private void editSelectedProduct() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentProducts.size()) {
            GamingTheme.showError(this, "Vui lòng chọn một sản phẩm để chỉnh sửa.");
            return;
        }
        openProductDialog(currentProducts.get(row));
    }

    private void quickUpdateStock() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentProducts.size()) {
            GamingTheme.showError(this, "Vui lòng chọn sản phẩm cần cập nhật tồn kho.");
            return;
        }
        Product p = currentProducts.get(row);
        String input = JOptionPane.showInputDialog(this,
                "Nhập số lượng tồn kho mới cho '" + p.getName() + "' (hiện tại: " + p.getStockQuantity() + "):",
                p.getStockQuantity());
        if (input == null) {
            return;
        }
        try {
            int newQty = Integer.parseInt(input.trim());
            inventoryService.setStockQuantity(p.getId(), newQty, "Cập nhật nhanh từ màn hình Sản phẩm");
            refreshData();
            GamingTheme.showSuccess(this, "Đã cập nhật tồn kho thành công.");
        } catch (NumberFormatException ex) {
            GamingTheme.showError(this, "Số lượng tồn kho phải là số nguyên hợp lệ.");
        } catch (Exception ex) {
            GamingTheme.showError(this, ex.getMessage());
        }
    }

    private void deleteSingleSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentProducts.size()) {
            GamingTheme.showError(this, "Vui lòng chọn sản phẩm cần xóa.");
            return;
        }
        Product p = currentProducts.get(row);
        if (!GamingTheme.showConfirm(this, "Bạn có chắc chắn muốn xóa sản phẩm '" + p.getName() + "'?")) {
            return;
        }
        try {
            productService.deleteProduct(p.getId());
            refreshData();
            GamingTheme.showSuccess(this, "Đã xóa sản phẩm " + p.getId() + ".");
        } catch (Exception ex) {
            GamingTheme.showError(this, ex.getMessage());
        }
    }

    private void deleteMultipleSelected() {
        int[] rows = table.getSelectedRows();
        if (rows == null || rows.length == 0) {
            GamingTheme.showError(this, "Vui lòng chọn ít nhất một sản phẩm để xóa.");
            return;
        }
        List<String> ids = new ArrayList<>();
        for (int r : rows) {
            if (r >= 0 && r < currentProducts.size()) {
                ids.add(currentProducts.get(r).getId());
            }
        }
        if (!GamingTheme.showConfirm(this, "Xác nhận xóa " + ids.size() + " sản phẩm đã chọn?")) {
            return;
        }
        try {
            int deleted = productService.deleteProducts(ids);
            refreshData();
            GamingTheme.showSuccess(this, "Đã xóa " + deleted + " sản phẩm.");
        } catch (Exception ex) {
            GamingTheme.showError(this, ex.getMessage());
        }
    }

    private void openProductDialog(Product existing) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                existing == null ? "Thêm sản phẩm mới" : "Chỉnh sửa sản phẩm",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(520, 500);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridLayout(8, 2, 10, 10));
        form.setBackground(GamingTheme.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JTextField nameField = GamingTheme.createTextField(20);
        JComboBox<Category> catCombo = new JComboBox<>();
        for (Category c : categoryService.getAllCategories()) {
            catCombo.addItem(c);
        }
        JTextField brandField = GamingTheme.createTextField(20);
        JTextField priceField = GamingTheme.createTextField(20);
        JTextField stockField = GamingTheme.createTextField(20);
        JTextField imageField = GamingTheme.createTextField(20);
        JTextField descField = GamingTheme.createTextField(20);
        JComboBox<ProductStatus> statusCombo = new JComboBox<>(ProductStatus.values());

        if (existing != null) {
            nameField.setText(existing.getName());
            for (int i = 0; i < catCombo.getItemCount(); i++) {
                if (catCombo.getItemAt(i).getId().equals(existing.getCategoryId())) {
                    catCombo.setSelectedIndex(i);
                    break;
                }
            }
            brandField.setText(existing.getBrand());
            priceField.setText(existing.getPrice().toPlainString());
            stockField.setText(String.valueOf(existing.getStockQuantity()));
            imageField.setText(existing.getImagePath());
            descField.setText(existing.getDescription());
            statusCombo.setSelectedItem(existing.getStatus());
        } else {
            imageField.setText("assets/products/gear_item.png");
            stockField.setText("10");
        }

        form.add(GamingTheme.createFieldLabel("Tên sản phẩm *"));
        form.add(nameField);
        form.add(GamingTheme.createFieldLabel("Danh mục *"));
        form.add(catCombo);
        form.add(GamingTheme.createFieldLabel("Thương hiệu *"));
        form.add(brandField);
        form.add(GamingTheme.createFieldLabel("Giá bán (VNĐ) *"));
        form.add(priceField);
        form.add(GamingTheme.createFieldLabel("Số lượng tồn kho *"));
        form.add(stockField);
        form.add(GamingTheme.createFieldLabel("Đường dẫn hình ảnh"));
        form.add(imageField);
        form.add(GamingTheme.createFieldLabel("Mô tả"));
        form.add(descField);
        form.add(GamingTheme.createFieldLabel("Trạng thái"));
        form.add(statusCombo);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setBackground(GamingTheme.BG_CARD);
        JButton cancelBtn = GamingTheme.createSecondaryButton("Hủy");
        cancelBtn.addActionListener(e -> dialog.dispose());
        JButton saveBtn = GamingTheme.createPrimaryButton("Lưu sản phẩm");
        saveBtn.addActionListener(e -> {
            try {
                Category selectedCategory = (Category) catCombo.getSelectedItem();
                if (selectedCategory == null) {
                    throw new IllegalArgumentException("Vui lòng chọn danh mục.");
                }
                BigDecimal price = new BigDecimal(priceField.getText().trim());
                int stock = Integer.parseInt(stockField.getText().trim());
                ProductStatus status = (ProductStatus) statusCombo.getSelectedItem();

                if (existing == null) {
                    productService.createProduct(
                            nameField.getText(), selectedCategory.getId(), brandField.getText(),
                            price, stock, descField.getText(), imageField.getText(), status
                    );
                    GamingTheme.showSuccess(dialog, "Đã thêm sản phẩm mới.");
                } else {
                    productService.updateProduct(
                            existing.getId(), nameField.getText(), selectedCategory.getId(), brandField.getText(),
                            price, stock, descField.getText(), imageField.getText(), status
                    );
                    GamingTheme.showSuccess(dialog, "Đã cập nhật sản phẩm.");
                }
                dialog.dispose();
                refreshData();
            } catch (NumberFormatException nfe) {
                GamingTheme.showError(dialog, "Giá bán và số lượng tồn kho phải là số hợp lệ.");
            } catch (Exception ex) {
                GamingTheme.showError(dialog, ex.getMessage());
            }
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);

        dialog.setLayout(new BorderLayout());
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private BigDecimal parseOptionalDecimal(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " phải là số hợp lệ.");
        }
    }
}
