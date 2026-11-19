package com.gaminggear;

import com.gaminggear.config.DependencyContainer;
import com.gaminggear.ui.LoginFrame;
import com.gaminggear.ui.theme.GamingTheme;

import javax.swing.*;

/**
 * Application entry point for GAMING GEAR MANAGEMENT SYSTEM.
 */
public class Main {

    public static void main(String[] args) {
        GamingTheme.setup();
        DependencyContainer container = new DependencyContainer();
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame(container);
            loginFrame.setVisible(true);
        });
    }
}
