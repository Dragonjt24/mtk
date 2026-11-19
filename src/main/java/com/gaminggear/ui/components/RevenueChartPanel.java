package com.gaminggear.ui.components;

import com.gaminggear.ui.theme.GamingTheme;
import com.gaminggear.util.CurrencyUtil;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom Java2D Bar Chart showing revenue breakdown dynamically.
 */
public class RevenueChartPanel extends JPanel {

    private Map<String, BigDecimal> data = new LinkedHashMap<>();
    private String chartTitle = "Doanh thu 7 ngày gần nhất";

    public RevenueChartPanel() {
        setBackground(GamingTheme.BG_CARD);
        setBorder(GamingTheme.createCardBorder());
        setPreferredSize(new Dimension(500, 250));
    }

    public void setData(String title, Map<String, BigDecimal> data) {
        this.chartTitle = title;
        this.data = data == null ? new LinkedHashMap<>() : new LinkedHashMap<>(data);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        g2.setColor(GamingTheme.TEXT_PRIMARY);
        g2.setFont(GamingTheme.FONT_SUBTITLE);
        g2.drawString(chartTitle, 18, 28);

        if (data.isEmpty()) {
            g2.setColor(GamingTheme.TEXT_MUTED);
            g2.setFont(GamingTheme.FONT_BODY);
            g2.drawString("Chưa có dữ liệu doanh thu.", 18, height / 2);
            g2.dispose();
            return;
        }

        int leftPad = 24;
        int rightPad = 24;
        int topPad = 52;
        int bottomPad = 42;
        int chartW = Math.max(100, width - leftPad - rightPad);
        int chartH = Math.max(60, height - topPad - bottomPad);

        BigDecimal maxVal = data.values().stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);
        if (maxVal.compareTo(BigDecimal.ZERO) <= 0) {
            maxVal = BigDecimal.ONE;
        }

        // Draw horizontal grid lines
        g2.setColor(new Color(51, 65, 85, 110));
        for (int i = 0; i <= 4; i++) {
            int y = topPad + (chartH * i) / 4;
            g2.drawLine(leftPad, y, width - rightPad, y);
        }

        int count = data.size();
        int slotW = chartW / Math.max(1, count);
        int barW = Math.max(16, Math.min(46, slotW - 18));

        int idx = 0;
        for (Map.Entry<String, BigDecimal> entry : data.entrySet()) {
            BigDecimal val = entry.getValue() == null ? BigDecimal.ZERO : entry.getValue();
            double ratio = val.doubleValue() / maxVal.doubleValue();
            int barH = (int) Math.round(ratio * (chartH - 12));
            int x = leftPad + idx * slotW + (slotW - barW) / 2;
            int y = topPad + chartH - barH;

            GradientPaint gp = new GradientPaint(
                    x, y, GamingTheme.ACCENT_BLUE,
                    x, topPad + chartH, GamingTheme.ACCENT_PURPLE
            );
            g2.setPaint(gp);
            g2.fillRoundRect(x, y, barW, Math.max(barH, 3), 8, 8);

            // Value above bar when > 0
            if (val.compareTo(BigDecimal.ZERO) > 0) {
                g2.setColor(GamingTheme.TEXT_PRIMARY);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                String shortVal = formatCompact(val);
                int strW = g2.getFontMetrics().stringWidth(shortVal);
                g2.drawString(shortVal, x + (barW - strW) / 2, Math.max(topPad - 2, y - 5));
            }

            // Label below bar
            g2.setColor(GamingTheme.TEXT_SECONDARY);
            g2.setFont(GamingTheme.FONT_SMALL);
            String label = entry.getKey();
            if (label.length() > 10) {
                label = label.substring(0, 9) + ".";
            }
            int lblW = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, x + (barW - lblW) / 2, topPad + chartH + 20);

            idx++;
        }

        g2.dispose();
    }

    private String formatCompact(BigDecimal val) {
        double d = val.doubleValue();
        if (d >= 1_000_000) {
            return String.format("%.1fM", d / 1_000_000.0);
        }
        if (d >= 1_000) {
            return String.format("%.0fK", d / 1_000.0);
        }
        return CurrencyUtil.formatNumber(val);
    }
}
