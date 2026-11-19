package com.gaminggear.ui.components;

import com.gaminggear.ui.theme.GamingTheme;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Left navigation sidebar switching CardLayout views inside MainFrame.
 */
public class SidebarPanel extends JPanel {

    public record NavItem(String key, String label) {
    }

    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Consumer<String> onNavigate;
    private final Runnable onLogout;
    private String activeKey = "DASHBOARD";

    public SidebarPanel(Consumer<String> onNavigate, Runnable onLogout) {
        this.onNavigate = onNavigate;
        this.onLogout = onLogout;

        setPreferredSize(new Dimension(235, 0));
        setBackground(GamingTheme.BG_SIDEBAR);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, GamingTheme.BORDER_SUBTLE));

        // Brand Header
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setBackground(GamingTheme.BG_SIDEBAR);
        brandPanel.setBorder(BorderFactory.createEmptyBorder(20, 18, 18, 18));

        JLabel logo = new JLabel("NEXUS GEAR");
        logo.setFont(new Font("SansSerif", Font.BOLD, 20));
        logo.setForeground(GamingTheme.ACCENT_BLUE);

        JLabel sub = new JLabel("Gaming Gear Admin");
        sub.setFont(GamingTheme.FONT_SMALL);
        sub.setForeground(GamingTheme.ACCENT_PURPLE);

        brandPanel.add(logo);
        brandPanel.add(Box.createVerticalStrut(4));
        brandPanel.add(sub);
        add(brandPanel, BorderLayout.NORTH);

        // Nav Menu
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(GamingTheme.BG_SIDEBAR);
        menuPanel.setBorder(BorderFactory.createEmptyBorder(6, 12, 12, 12));

        NavItem[] items = {
                new NavItem("DASHBOARD", "Dashboard"),
                new NavItem("PRODUCTS", "Products"),
                new NavItem("CATEGORIES", "Categories"),
                new NavItem("CUSTOMERS", "Customers"),
                new NavItem("ORDERS", "Orders"),
                new NavItem("CREATE_ORDER", "Create Order"),
                new NavItem("DISCOUNTS", "Discounts"),
                new NavItem("PAYMENTS", "Payments"),
                new NavItem("INVOICES", "Invoices"),
                new NavItem("INVENTORY", "Inventory"),
                new NavItem("SETTINGS", "Settings")
        };

        for (NavItem item : items) {
            JButton btn = createNavButton(item.key(), item.label());
            navButtons.put(item.key(), btn);
            menuPanel.add(btn);
            menuPanel.add(Box.createVerticalStrut(5));
        }

        JScrollPane menuScroll = new JScrollPane(menuPanel);
        menuScroll.setBorder(null);
        menuScroll.getViewport().setBackground(GamingTheme.BG_SIDEBAR);
        menuScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(menuScroll, BorderLayout.CENTER);

        // Footer Logout
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(GamingTheme.BG_SIDEBAR);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 12, 16, 12));

        JButton logoutBtn = GamingTheme.createDangerButton("Đăng xuất");
        logoutBtn.addActionListener(e -> {
            if (this.onLogout != null) {
                this.onLogout.run();
            }
        });
        footer.add(logoutBtn, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        setActive("DASHBOARD");
    }

    private JButton createNavButton(String key, String label) {
        JButton btn = new JButton("  " + label);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setPreferredSize(new Dimension(210, 38));
        btn.setFont(GamingTheme.FONT_BODY_BOLD);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            setActive(key);
            if (onNavigate != null) {
                onNavigate.accept(key);
            }
        });
        return btn;
    }

    public void setActive(String key) {
        this.activeKey = key;
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            JButton btn = entry.getValue();
            boolean isActive = entry.getKey().equals(activeKey);
            if (isActive) {
                btn.setBackground(new Color(37, 99, 235, 190));
                btn.setForeground(Color.WHITE);
                btn.setBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, GamingTheme.ACCENT_BLUE));
            } else {
                btn.setBackground(GamingTheme.BG_SIDEBAR);
                btn.setForeground(GamingTheme.TEXT_SECONDARY);
                btn.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
            }
        }
    }
}
