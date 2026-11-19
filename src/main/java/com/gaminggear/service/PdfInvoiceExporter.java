package com.gaminggear.service;

import com.gaminggear.config.AppConfig;
import com.gaminggear.model.Bill;
import com.gaminggear.model.BillItem;
import com.gaminggear.util.CurrencyUtil;
import com.gaminggear.util.DateTimeUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.List;

/**
 * Handles PDF export (via OpenPDF) and Java desktop printing for generated Bills.
 */
public class PdfInvoiceExporter {

    private static final List<String> CANDIDATE_UNICODE_FONTS = List.of(
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
            "C:\\Windows\\Fonts\\arial.ttf",
            "C:\\Windows\\Fonts\\segoeui.ttf",
            "/System/Library/Fonts/Supplemental/Arial.ttf"
    );

    public void exportToPdf(Bill bill, File targetFile) throws Exception {
        if (bill == null) {
            throw new IllegalArgumentException("Hóa đơn không được null.");
        }
        if (targetFile == null) {
            throw new IllegalArgumentException("Đường dẫn tệp PDF không hợp lệ.");
        }
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        BaseFont baseFont = resolveBaseFont();
        boolean asciiFallback = baseFont == null;

        Font titleFont = asciiFallback
                ? new Font(Font.HELVETICA, 18, Font.BOLD, new Color(37, 99, 235))
                : new Font(baseFont, 18, Font.BOLD, new Color(37, 99, 235));
        Font headerFont = asciiFallback
                ? new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE)
                : new Font(baseFont, 11, Font.BOLD, Color.WHITE);
        Font boldFont = asciiFallback
                ? new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK)
                : new Font(baseFont, 10, Font.BOLD, Color.BLACK);
        Font normalFont = asciiFallback
                ? new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY)
                : new Font(baseFont, 10, Font.NORMAL, Color.DARK_GRAY);

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        try (FileOutputStream out = new FileOutputStream(targetFile)) {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph storeTitle = new Paragraph(safeText(AppConfig.STORE_NAME, asciiFallback), titleFont);
            storeTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(storeTitle);

            Paragraph storeMeta = new Paragraph(
                    safeText(AppConfig.STORE_ADDRESS + " | Hotline: " + AppConfig.STORE_PHONE, asciiFallback),
                    normalFont
            );
            storeMeta.setAlignment(Element.ALIGN_CENTER);
            storeMeta.setSpacingAfter(14);
            document.add(storeMeta);

            Paragraph invoiceHeading = new Paragraph(
                    safeText("HÓA ĐƠN BÁN HÀNG - " + bill.getId(), asciiFallback), boldFont);
            invoiceHeading.setSpacingAfter(8);
            document.add(invoiceHeading);

            document.add(new Paragraph(safeText("Mã đơn hàng: " + bill.getOrderId(), asciiFallback), normalFont));
            document.add(new Paragraph(safeText("Ngày lập hóa đơn: " + DateTimeUtil.format(bill.getIssuedAt()), asciiFallback), normalFont));
            document.add(new Paragraph(safeText("Khách hàng: " + bill.getCustomerName() + " (" + bill.getCustomerPhone() + ")", asciiFallback), normalFont));
            document.add(new Paragraph(safeText("Địa chỉ: " + bill.getCustomerAddress(), asciiFallback), normalFont));
            document.add(new Paragraph(safeText("Phương thức thanh toán: " + bill.getPaymentMethod(), asciiFallback), normalFont));
            Paragraph statusPara = new Paragraph(safeText("Trạng thái thanh toán: " + bill.getPaymentStatus(), asciiFallback), boldFont);
            statusPara.setSpacingAfter(12);
            document.add(statusPara);

            PdfPTable table = new PdfPTable(new float[]{1.2f, 4.2f, 1.2f, 2.2f, 2.4f});
            table.setWidthPercentage(100);

            String[] headers = {"STT", "Sản phẩm", "SL", "Đơn giá", "Thành tiền"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(safeText(h, asciiFallback), headerFont));
                cell.setBackgroundColor(new Color(30, 41, 59));
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            int idx = 1;
            for (BillItem item : bill.getItems()) {
                table.addCell(createCell(String.valueOf(idx++), normalFont, Element.ALIGN_CENTER));
                table.addCell(createCell(safeText(item.getProductName(), asciiFallback), normalFont, Element.ALIGN_LEFT));
                table.addCell(createCell(String.valueOf(item.getQuantity()), normalFont, Element.ALIGN_CENTER));
                table.addCell(createCell(safeText(CurrencyUtil.formatVnd(item.getUnitPrice()), asciiFallback), normalFont, Element.ALIGN_RIGHT));
                table.addCell(createCell(safeText(CurrencyUtil.formatVnd(item.getLineTotal()), asciiFallback), normalFont, Element.ALIGN_RIGHT));
            }
            table.setSpacingAfter(12);
            document.add(table);

            Paragraph orig = new Paragraph(safeText("Tổng tiền ban đầu: " + CurrencyUtil.formatVnd(bill.getOriginalTotal()), asciiFallback), normalFont);
            orig.setAlignment(Element.ALIGN_RIGHT);
            document.add(orig);

            Paragraph disc = new Paragraph(safeText(
                    "Giảm giá (" + CurrencyUtil.formatPercent(bill.getDiscountRate()) + "): -"
                            + CurrencyUtil.formatVnd(bill.getDiscountAmount()), asciiFallback), normalFont);
            disc.setAlignment(Element.ALIGN_RIGHT);
            document.add(disc);

            Paragraph total = new Paragraph(safeText(
                    "Tổng tiền thanh toán: " + CurrencyUtil.formatVnd(bill.getFinalTotal()), asciiFallback), titleFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            total.setSpacingBefore(4);
            document.add(total);

            document.close();
        }
    }

    public boolean printBill(Bill bill) throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("HoaDon_" + bill.getId());
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) {
                return Printable.NO_SUCH_PAGE;
            }
            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            int y = 24;
            int step = 18;
            g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
            g2d.drawString(AppConfig.STORE_NAME, 20, y);
            y += step;
            g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
            g2d.drawString("Hóa đơn: " + bill.getId() + " | Đơn hàng: " + bill.getOrderId(), 20, y);
            y += step;
            g2d.drawString("Khách hàng: " + bill.getCustomerName() + " - " + bill.getCustomerPhone(), 20, y);
            y += step;
            g2d.drawString("Ngày lập: " + DateTimeUtil.format(bill.getIssuedAt()), 20, y);
            y += step * 2;

            g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 10));
            g2d.drawString("Sản phẩm", 20, y);
            g2d.drawString("SL", 260, y);
            g2d.drawString("Đơn giá", 310, y);
            g2d.drawString("Thành tiền", 400, y);
            y += step;
            g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
            for (BillItem item : bill.getItems()) {
                g2d.drawString(truncate(item.getProductName(), 36), 20, y);
                g2d.drawString(String.valueOf(item.getQuantity()), 260, y);
                g2d.drawString(CurrencyUtil.formatNumber(item.getUnitPrice()), 310, y);
                g2d.drawString(CurrencyUtil.formatNumber(item.getLineTotal()), 400, y);
                y += step;
            }
            y += step;
            g2d.drawString("Tổng ban đầu: " + CurrencyUtil.formatVnd(bill.getOriginalTotal()), 20, y);
            y += step;
            g2d.drawString("Giảm giá: -" + CurrencyUtil.formatVnd(bill.getDiscountAmount()), 20, y);
            y += step;
            g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
            g2d.drawString("Tổng thanh toán: " + CurrencyUtil.formatVnd(bill.getFinalTotal()), 20, y);
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            job.print();
            return true;
        }
        return false;
    }

    private PdfPCell createCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(align);
        return cell;
    }

    private BaseFont resolveBaseFont() {
        for (String fontPath : CANDIDATE_UNICODE_FONTS) {
            try {
                if (Files.exists(Path.of(fontPath))) {
                    return BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String safeText(String text, boolean asciiFallback) {
        if (text == null) {
            return "";
        }
        if (!asciiFallback) {
            return text;
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return normalized;
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
