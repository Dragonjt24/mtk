package com.gaminggear.ui.components;

import com.gaminggear.model.enums.OrderStatus;
import com.gaminggear.ui.theme.GamingTheme;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom Java2D chart rendering order status distribution (PENDING, PAID, CANCELLED).
 */
public class OrderStatusChartPanel extends JPanel {

    private Map<OrderStatus, Integer> statusCounts = new LinkedHashMap<>();

    public OrderStatusChartPanel() {
        setBackground(GamingTheme.BG_CARD);
        setBorder(GamingTheme.createCardBorder());
        setPreferredSize(new Dimension(380, 250));
    }

    public void setStatusCounts(Map<OrderStatus, Integer> statusCounts) {
        this.statusCounts = statusCounts == null ? new LinkedHashMap<>() : new LinkedHashMap<>(statusCounts);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(GamingTheme.TEXT_PRIMARY);
        g2.setFont(GamingTheme.FONT_SUBTITLE);
        g2.drawString("Đơn hàng theo trạng thái", 18, 28);

        int pending = statusCounts.getOrDefault(OrderStatus.PENDING, 0);
        int paid = statusCounts.getOrDefault(OrderStatus.PAID, 0);
        int cancelled = statusCounts.getOrDefault(OrderStatus.CANCELLED, 0);
        int total = pending + paid + cancelled;

        int diameter = Math.min(getHeight() - 80, 145);
        int cx = 30;
        int cy = 58;

        if (total == 0) {
            g2.setColor(GamingTheme.BG_ELEVATED);
            g2.fillOval(cx, cy, diameter, diameter);
        } else {
            int startAngle = 90;
            startAngle = drawArc(g2, cx, cy, diameter, startAngle, paid, total, GamingTheme.SUCCESS);
            startAngle = drawArc(g2, cx, cy, diameter, startAngle, pending, total, GamingTheme.WARNING);
            drawArc(g2, cx, cy, diameter, startAngle, cancelled, total, GamingTheme.DANGER);
        }

        // Donut hole
        int hole = (int) (diameter * 0.56);
        int hx = cx + (diameter - hole) / 2;
        int hy = cy + (diameter - hole) / 2;
        g2.setColor(GamingTheme.BG_CARD);
        g2.fillOval(hx, hy, hole, hole);

        g2.setColor(GamingTheme.TEXT_PRIMARY);
        g2.setFont(GamingTheme.FONT_SUBTITLE);
        String totalStr = String.valueOf(total);
        int tw = g2.getFontMetrics().stringWidth(totalStr);
        g2.drawString(totalStr, hx + (hole - tw) / 2, hy + hole / 2 + 5);

        // Legend
        int lx = cx + diameter + 28;
        int ly = 82;
        drawLegendItem(g2, lx, ly, GamingTheme.SUCCESS, "Đã thanh toán (PAID)", paid);
        drawLegendItem(g2, lx, ly + 32, GamingTheme.WARNING, "Chờ xử lý (PENDING)", pending);
        drawLegendItem(g2, lx, ly + 64, GamingTheme.DANGER, "Đã hủy (CANCELLED)", cancelled);

        g2.dispose();
    }

    private int drawArc(Graphics2D g2, int x, int y, int d, int startAngle, int value, int total, Color color) {
        if (value <= 0) {
            return startAngle;
        }
        int angle = (int) Math.round((value * 360.0) / total);
        g2.setColor(color);
        g2.fillArc(x, y, d, d, startAngle, -angle);
        return startAngle - angle;
    }

    private void drawLegendItem(Graphics2D g2, int x, int y, Color color, String label, int count) {
        g2.setColor(color);
        g2.fillRoundRect(x, y - 10, 12, 12, 4, 4);
        g2.setColor(GamingTheme.TEXT_PRIMARY);
        g2.setFont(GamingTheme.FONT_BODY);
        g2.drawString(label + ": " + count, x + 20, y);
    }
}
