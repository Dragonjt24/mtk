package com.gaminggear.ui.components;

import com.gaminggear.model.User;
import com.gaminggear.patterns.observer.NotificationObserver;
import com.gaminggear.ui.theme.GamingTheme;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Top header bar displaying page title, notification history popup, refresh trigger, and active user info.
 */
public class HeaderPanel extends JPanel {

    private final JLabel pageTitleLabel;
    private final JLabel userBadgeLabel;
    private final JButton notificationButton;
    private final NotificationObserver notificationObserver;

    public HeaderPanel(NotificationObserver notificationObserver, Runnable onRefresh) {
        this.notificationObserver = notificationObserver;

        setPreferredSize(new Dimension(0, 62));
        setBackground(GamingTheme.BG_SIDEBAR);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, GamingTheme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(10, 22, 10, 22)
        ));

        pageTitleLabel = new JLabel("Dashboard");
        pageTitleLabel.setFont(GamingTheme.FONT_TITLE);
        pageTitleLabel.setForeground(GamingTheme.TEXT_PRIMARY);
        add(pageTitleLabel, BorderLayout.WEST);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightControls.setOpaque(false);

        JButton refreshBtn = GamingTheme.createSecondaryButton("Làm mới");
        refreshBtn.addActionListener(e -> {
            if (onRefresh != null) {
                onRefresh.run();
            }
        });

        notificationButton = GamingTheme.createNeonButton("Thông báo (0)");
        notificationButton.addActionListener(e -> showNotificationHistory());

        userBadgeLabel = new JLabel("User");
        userBadgeLabel.setFont(GamingTheme.FONT_BODY_BOLD);
        userBadgeLabel.setForeground(GamingTheme.ACCENT_BLUE);
        userBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GamingTheme.BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        rightControls.add(refreshBtn);
        rightControls.add(notificationButton);
        rightControls.add(userBadgeLabel);

        add(rightControls, BorderLayout.EAST);
    }

    public void setPageTitle(String title) {
        pageTitleLabel.setText(title);
    }

    public void setCurrentUser(User user) {
        if (user == null) {
            userBadgeLabel.setText("Guest");
        } else {
            userBadgeLabel.setText(user.getUsername() + " [" + user.getRole().name() + "]");
        }
    }

    public void updateNotificationCount() {
        int count = notificationObserver == null ? 0 : notificationObserver.getNotifications().size();
        notificationButton.setText("Thông báo (" + count + ")");
    }

    private void showNotificationHistory() {
        List<String> items = notificationObserver.getNotifications();
        if (items.isEmpty()) {
            GamingTheme.showSuccess(this, "Chưa có thông báo thanh toán nào trong phiên làm việc này.");
            return;
        }
        JTextArea area = new JTextArea(String.join("\n\n", items));
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(GamingTheme.BG_INPUT);
        area.setForeground(GamingTheme.TEXT_PRIMARY);
        area.setFont(GamingTheme.FONT_BODY);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(560, 260));
        JOptionPane.showMessageDialog(this, scroll, "Nhật ký sự kiện thanh toán (NotificationObserver)",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
