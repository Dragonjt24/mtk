package com.gaminggear.ui.invoice;

import com.gaminggear.config.AppConfig;
import com.gaminggear.model.Bill;
import com.gaminggear.model.BillItem;
import com.gaminggear.service.BillService;
import com.gaminggear.service.PdfInvoiceExporter;
import com.gaminggear.ui.components.RefreshablePanel;
import com.gaminggear.ui.theme.GamingTheme;
import com.gaminggear.util.CurrencyUtil;
import com.gaminggear.util.DateTimeUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen 10: Invoice (Bill) viewer with Print and OpenPDF export support.
 */
public class InvoicePanel extends JPanel implements RefreshablePanel {

    private final BillService billService;
    private final PdfInvoiceExporter pdfExporter;

    private final DefaultTableModel billListModel;
    private final JTable billTable;

    private final JLabel billHeaderLabel;
    private final JTextArea customerInfoArea;
    private final DefaultTableModel billItemModel;
    private final JLabel totalsSummaryLabel;

    private List<Bill> bills = new ArrayList<>();
    private Bill selectedBill = null;

    public InvoicePanel(BillService billService, PdfInvoiceExporter pdfExporter) {
        this.billService = billService;
        this.pdfExporter = pdfExporter;

        setLayout(new GridLayout(1, 2, 16, 0));
        setBackground(GamingTheme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Left: Bills Table
        JPanel leftCard = GamingTheme.createCardPanel();
        leftCard.setLayout(new BorderLayout(0, 10));
        leftCard.add(GamingTheme.createSectionTitle("Danh sách hóa đơn đã phát hành"), BorderLayout.NORTH);

        billListModel = new DefaultTableModel(
                new String[]{"Mã hóa đơn", "Mã đơn", "Khách hàng", "Tổng thanh toán", "Ngày lập", "Trạng thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        billTable = new JTable(billListModel);
        billTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        billTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedBillPreview();
            }
        });
        leftCard.add(GamingTheme.wrapTable(billTable), BorderLayout.CENTER);

        // Right: Formatted Invoice Preview + Print / Export PDF
        JPanel rightCard = GamingTheme.createCardPanel();
        rightCard.setLayout(new BorderLayout(0, 12));

        JPanel previewHeader = new JPanel(new BorderLayout());
        previewHeader.setOpaque(false);
        billHeaderLabel = GamingTheme.createSectionTitle("Chi tiết hóa đơn");
        previewHeader.add(billHeaderLabel, BorderLayout.WEST);

        JPanel exportBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        exportBtns.setOpaque(false);
        JButton printBtn = GamingTheme.createSecondaryButton("In hóa đơn");
        printBtn.addActionListener(e -> printSelectedBill());
        JButton pdfBtn = GamingTheme.createNeonButton("Xuất PDF");
        pdfBtn.addActionListener(e -> exportSelectedBillPdf());
        exportBtns.add(printBtn);
        exportBtns.add(pdfBtn);
        previewHeader.add(exportBtns, BorderLayout.EAST);

        customerInfoArea = new JTextArea(5, 30);
        customerInfoArea.setEditable(false);
        customerInfoArea.setBackground(GamingTheme.BG_ELEVATED);
        customerInfoArea.setForeground(GamingTheme.TEXT_PRIMARY);
        customerInfoArea.setFont(GamingTheme.FONT_BODY);
        customerInfoArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        billItemModel = new DefaultTableModel(
                new String[]{"Sản phẩm", "SL", "Đơn giá", "Thành tiền"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable itemTable = new JTable(billItemModel);

        totalsSummaryLabel = new JLabel("Chọn hóa đơn bên trái để xem chi tiết.");
        totalsSummaryLabel.setFont(GamingTheme.FONT_BODY_BOLD);
        totalsSummaryLabel.setForeground(GamingTheme.ACCENT_BLUE);
        totalsSummaryLabel.setBorder(BorderFactory.createEmptyBorder(8, 6, 4, 6));

        JPanel centerPreview = new JPanel(new BorderLayout(0, 10));
        centerPreview.setOpaque(false);
        centerPreview.add(customerInfoArea, BorderLayout.NORTH);
        centerPreview.add(GamingTheme.wrapTable(itemTable), BorderLayout.CENTER);
        centerPreview.add(totalsSummaryLabel, BorderLayout.SOUTH);

        rightCard.add(previewHeader, BorderLayout.NORTH);
        rightCard.add(centerPreview, BorderLayout.CENTER);

        add(leftCard);
        add(rightCard);

        refreshData();
    }

    @Override
    public void refreshData() {
        bills = billService.getAllBills();
        billListModel.setRowCount(0);
        for (Bill b : bills) {
            billListModel.addRow(new Object[]{
                    b.getId(),
                    b.getOrderId(),
                    b.getCustomerName(),
                    CurrencyUtil.formatVnd(b.getFinalTotal()),
                    DateTimeUtil.format(b.getIssuedAt()),
                    b.getPaymentStatus().name()
            });
        }
        if (!bills.isEmpty() && billTable.getSelectedRow() < 0) {
            billTable.setRowSelectionInterval(0, 0);
        } else {
            loadSelectedBillPreview();
        }
    }

    public void selectBillById(String billId) {
        refreshData();
        for (int i = 0; i < bills.size(); i++) {
            if (bills.get(i).getId().equals(billId)) {
                billTable.setRowSelectionInterval(i, i);
                break;
            }
        }
    }

    private void loadSelectedBillPreview() {
        int row = billTable.getSelectedRow();
        billItemModel.setRowCount(0);
        if (row < 0 || row >= bills.size()) {
            selectedBill = null;
            billHeaderLabel.setText("Chi tiết hóa đơn");
            customerInfoArea.setText("Chưa chọn hóa đơn.");
            totalsSummaryLabel.setText(" ");
            return;
        }

        selectedBill = bills.get(row);
        billHeaderLabel.setText("Hóa đơn: " + selectedBill.getId());
        customerInfoArea.setText(String.format(
                "%s | %s%nMã đơn hàng: %s | Ngày lập: %s%nKhách hàng: %s (%s)%nĐịa chỉ: %s%nPhương thức: %s | Trạng thái: %s",
                AppConfig.STORE_NAME, AppConfig.STORE_PHONE,
                selectedBill.getOrderId(), DateTimeUtil.format(selectedBill.getIssuedAt()),
                selectedBill.getCustomerName(), selectedBill.getCustomerPhone(),
                selectedBill.getCustomerAddress(),
                selectedBill.getPaymentMethod().getDisplayName(),
                selectedBill.getPaymentStatus().getDisplayName()
        ));

        for (BillItem item : selectedBill.getItems()) {
            billItemModel.addRow(new Object[]{
                    item.getProductName(),
                    item.getQuantity(),
                    CurrencyUtil.formatVnd(item.getUnitPrice()),
                    CurrencyUtil.formatVnd(item.getLineTotal())
            });
        }

        totalsSummaryLabel.setText(String.format(
                "<html>Giá gốc: %s &nbsp;&nbsp;|&nbsp;&nbsp; Giảm giá (%s): -%s &nbsp;&nbsp;|&nbsp;&nbsp; <font color='#10B981'><b>TỔNG THANH TOÁN: %s</b></font></html>",
                CurrencyUtil.formatVnd(selectedBill.getOriginalTotal()),
                CurrencyUtil.formatPercent(selectedBill.getDiscountRate()),
                CurrencyUtil.formatVnd(selectedBill.getDiscountAmount()),
                CurrencyUtil.formatVnd(selectedBill.getFinalTotal())
        ));
    }

    private void printSelectedBill() {
        if (selectedBill == null) {
            GamingTheme.showError(this, "Vui lòng chọn hóa đơn cần in.");
            return;
        }
        try {
            boolean printed = pdfExporter.printBill(selectedBill);
            if (printed) {
                GamingTheme.showSuccess(this, "Đã gửi lệnh in hóa đơn " + selectedBill.getId() + ".");
            }
        } catch (Exception ex) {
            GamingTheme.showError(this, "Không thể in hóa đơn: " + ex.getMessage());
        }
    }

    private void exportSelectedBillPdf() {
        if (selectedBill == null) {
            GamingTheme.showError(this, "Vui lòng chọn hóa đơn cần xuất PDF.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Lưu hóa đơn PDF");
        chooser.setSelectedFile(new File(selectedBill.getId() + ".pdf"));
        int choice = chooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase().endsWith(".pdf")) {
            target = new File(target.getAbsolutePath() + ".pdf");
        }
        try {
            pdfExporter.exportToPdf(selectedBill, target);
            GamingTheme.showSuccess(this, "Đã xuất hóa đơn PDF thành công tại:\n" + target.getAbsolutePath());
        } catch (Exception ex) {
            GamingTheme.showError(this, "Xuất PDF thất bại: " + ex.getMessage());
        }
    }
}
