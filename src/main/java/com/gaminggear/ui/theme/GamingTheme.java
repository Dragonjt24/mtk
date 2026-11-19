package com.gaminggear.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Unified Dark Mode + Neon Blue/Purple design system for Gaming Gear Management System.
 */
public final class GamingTheme {

    public static final Color BG_APP = new Color(11, 15, 25);
    public static final Color BG_SIDEBAR = new Color(15, 23, 42);
    public static final Color BG_CARD = new Color(22, 30, 46);
    public static final Color BG_ELEVATED = new Color(30, 41, 59);
    public static final Color BG_INPUT = new Color(17, 24, 39);
    public static final Color BORDER_SUBTLE = new Color(51, 65, 85);

    public static final Color ACCENT_BLUE = new Color(56, 189, 248);
    public static final Color ACCENT_BLUE_DARK = new Color(37, 99, 235);
    public static final Color ACCENT_PURPLE = new Color(139, 92, 246);
    public static final Color ACCENT_PURPLE_HOVER = new Color(124, 58, 237);

    public static final Color SUCCESS = new Color(16, 185, 129);
    public static final Color WARNING = new Color(245, 158, 11);
    public static final Color DANGER = new Color(239, 68, 68);
    public static final Color DANGER_HOVER = new Color(220, 38, 38);

    public static final Color TEXT_PRIMARY = new Color(241, 245, 249);
    public static final Color TEXT_SECONDARY = new Color(148, 163, 184);
    public static final Color TEXT_MUTED = new Color(100, 116, 139);

    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("SansSerif", Font.BOLD, 16);
    public static final Font FONT_CARD_VALUE = new Font("SansSerif", Font.BOLD, 24);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 12);

    private GamingTheme() {
    }

    public static void setup() {
        try {
            FlatDarkLaf.setup();
            UIManager.put("Panel.background", BG_APP);
            UIManager.put("OptionPane.background", BG_CARD);
            UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
            UIManager.put("TextField.background", BG_INPUT);
            UIManager.put("TextField.foreground", TEXT_PRIMARY);
            UIManager.put("PasswordField.background", BG_INPUT);
            UIManager.put("PasswordField.foreground", TEXT_PRIMARY);
            UIManager.put("TextArea.background", BG_INPUT);
            UIManager.put("TextArea.foreground", TEXT_PRIMARY);
            UIManager.put("ComboBox.background", BG_INPUT);
            UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
            UIManager.put("Spinner.background", BG_INPUT);
            UIManager.put("Spinner.foreground", TEXT_PRIMARY);
            UIManager.put("Table.background", BG_CARD);
            UIManager.put("Table.foreground", TEXT_PRIMARY);
            UIManager.put("Table.selectionBackground", new Color(37, 99, 235, 140));
            UIManager.put("Table.selectionForeground", Color.WHITE);
            UIManager.put("Table.gridColor", BORDER_SUBTLE);
            UIManager.put("ScrollPane.background", BG_CARD);
            UIManager.put("Component.arc", 10);
            UIManager.put("Button.arc", 10);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("Component.focusColor", ACCENT_BLUE);
        } catch (Exception ignored) {
        }
    }

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_CARD);
        panel.setBorder(createCardBorder());
        return panel;
    }

    public static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        );
    }

    public static JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SUBTITLE);
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    public static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BODY_BOLD);
        label.setForeground(TEXT_SECONDARY);
        return label;
    }

    public static JTextField createTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(FONT_BODY);
        field.setBackground(BG_INPUT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(ACCENT_BLUE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        return field;
    }

    public static JButton createPrimaryButton(String text) {
        return createStyledButton(text, ACCENT_BLUE_DARK, new Color(29, 78, 216), Color.WHITE);
    }

    public static JButton createNeonButton(String text) {
        return createStyledButton(text, ACCENT_PURPLE, ACCENT_PURPLE_HOVER, Color.WHITE);
    }

    public static JButton createSuccessButton(String text) {
        return createStyledButton(text, SUCCESS, new Color(5, 150, 105), Color.WHITE);
    }

    public static JButton createDangerButton(String text) {
        return createStyledButton(text, DANGER, DANGER_HOVER, Color.WHITE);
    }

    public static JButton createSecondaryButton(String text) {
        return createStyledButton(text, BG_ELEVATED, new Color(51, 65, 85), TEXT_PRIMARY);
    }

    public static JButton createStyledButton(String text, Color baseBg, Color hoverBg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setBackground(baseBg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(hoverBg);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(baseBg);
            }
        });
        return btn;
    }

    public static void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setFont(FONT_BODY);
        table.setRowHeight(34);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(38, 49, 68));
        table.setSelectionBackground(new Color(37, 99, 235, 130));
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(BG_ELEVATED);
        header.setForeground(ACCENT_BLUE);
        header.setFont(FONT_BODY_BOLD);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer alternatingRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? BG_CARD : new Color(26, 35, 53));
                    c.setForeground(TEXT_PRIMARY);
                }
                if (c instanceof JLabel lbl) {
                    lbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                }
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, alternatingRenderer);
    }

    public static JScrollPane wrapTable(JTable table) {
        styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_SUBTLE, 1));
        scroll.getViewport().setBackground(BG_CARD);
        return scroll;
    }

    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Lỗi thao tác", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean showConfirm(Component parent, String message) {
        int result = JOptionPane.showConfirmDialog(parent, message, "Xác nhận",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }
}
