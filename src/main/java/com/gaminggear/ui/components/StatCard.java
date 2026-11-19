package com.gaminggear.ui.components;

import com.gaminggear.ui.theme.GamingTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Reusable metric card for Dashboard summary KPIs.
 */
public class StatCard extends JPanel {

    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel subLabel;
    private final Color accentColor;

    public StatCard(String title, String initialValue, String subtitle, Color accentColor) {
        this.accentColor = accentColor;
        setLayout(new BorderLayout(0, 8));
        setBackground(GamingTheme.BG_CARD);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(3, 1, 1, 1, accentColor),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(GamingTheme.FONT_SMALL);
        titleLabel.setForeground(GamingTheme.TEXT_SECONDARY);

        valueLabel = new JLabel(initialValue);
        valueLabel.setFont(GamingTheme.FONT_CARD_VALUE);
        valueLabel.setForeground(GamingTheme.TEXT_PRIMARY);

        subLabel = new JLabel(subtitle);
        subLabel.setFont(GamingTheme.FONT_SMALL);
        subLabel.setForeground(accentColor);

        add(titleLabel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
        add(subLabel, BorderLayout.SOUTH);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setSubtitle(String subtitle) {
        subLabel.setText(subtitle);
    }

    public Color getAccentColor() {
        return accentColor;
    }
}
