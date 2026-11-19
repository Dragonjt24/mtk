package com.gaminggear.ui;

import com.gaminggear.config.AppConfig;
import com.gaminggear.config.DependencyContainer;
import com.gaminggear.model.User;
import com.gaminggear.ui.theme.GamingTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Screen 1: Modern Dark Mode Login screen with Show/Hide password toggle and validation.
 */
public class LoginFrame extends JFrame {

    private final DependencyContainer container;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton togglePasswordBtn;
    private final JLabel errorLabel;
    private boolean passwordVisible = false;

    public LoginFrame(DependencyContainer container) {
        this.container = container;

        setTitle(AppConfig.APP_TITLE + " - Đăng nhập");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(520, 480);
        setMinimumSize(new Dimension(480, 440));
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(GamingTheme.BG_APP);
        root.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(GamingTheme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GamingTheme.ACCENT_BLUE_DARK, 1),
                BorderFactory.createEmptyBorder(28, 30, 28, 30)
        ));
        card.setPreferredSize(new Dimension(420, 385));

        JLabel brandLabel = new JLabel("NEXUS GAMING GEAR");
        brandLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        brandLabel.setForeground(GamingTheme.ACCENT_BLUE);
        brandLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel("Đăng nhập hệ thống quản lý");
        titleLabel.setFont(GamingTheme.FONT_BODY);
        titleLabel.setForeground(GamingTheme.TEXT_SECONDARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(brandLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(22));

        JLabel userLbl = GamingTheme.createFieldLabel("Tên đăng nhập");
        userLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameField = GamingTheme.createTextField(20);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel passLbl = GamingTheme.createFieldLabel("Mật khẩu");
        passLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel passRow = new JPanel(new BorderLayout(8, 0));
        passRow.setOpaque(false);
        passRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        passRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        passwordField = new JPasswordField();
        passwordField.setFont(GamingTheme.FONT_BODY);
        passwordField.setBackground(GamingTheme.BG_INPUT);
        passwordField.setForeground(GamingTheme.TEXT_PRIMARY);
        passwordField.setCaretColor(GamingTheme.ACCENT_BLUE);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GamingTheme.BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        char defaultEcho = passwordField.getEchoChar();

        togglePasswordBtn = GamingTheme.createSecondaryButton("Hiện");
        togglePasswordBtn.setPreferredSize(new Dimension(76, 38));
        togglePasswordBtn.addActionListener(e -> {
            passwordVisible = !passwordVisible;
            passwordField.setEchoChar(passwordVisible ? (char) 0 : defaultEcho);
            togglePasswordBtn.setText(passwordVisible ? "Ẩn" : "Hiện");
        });

        passRow.add(passwordField, BorderLayout.CENTER);
        passRow.add(togglePasswordBtn, BorderLayout.EAST);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(GamingTheme.FONT_SMALL);
        errorLabel.setForeground(GamingTheme.DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton loginButton = GamingTheme.createPrimaryButton("ĐĂNG NHẬP");
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.addActionListener(e -> performLogin());

        JLabel demoHint = new JLabel("Tài khoản demo: admin / admin123  (hoặc staff01 / staff123)");
        demoHint.setFont(GamingTheme.FONT_SMALL);
        demoHint.setForeground(GamingTheme.TEXT_MUTED);
        demoHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(userLbl);
        card.add(Box.createVerticalStrut(6));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(14));
        card.add(passLbl);
        card.add(Box.createVerticalStrut(6));
        card.add(passRow);
        card.add(Box.createVerticalStrut(10));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(loginButton);
        card.add(Box.createVerticalStrut(16));
        card.add(demoHint);

        root.add(card);
        setContentPane(root);
        getRootPane().setDefaultButton(loginButton);

        usernameField.setText("admin");
        passwordField.setText("admin123");
    }

    private void performLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        try {
            User user = container.getAuthService().login(username, password);
            errorLabel.setText(" ");
            MainFrame mainFrame = new MainFrame(container, user);
            mainFrame.setVisible(true);
            dispose();
        } catch (Exception ex) {
            errorLabel.setText(ex.getMessage());
            GamingTheme.showError(this, ex.getMessage());
        }
    }
}
