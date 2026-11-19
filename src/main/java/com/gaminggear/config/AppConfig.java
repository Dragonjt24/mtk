package com.gaminggear.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central application configuration constants and runtime settings.
 */
public final class AppConfig {

    public static final String APP_TITLE = "GAMING GEAR MANAGEMENT SYSTEM";
    public static final String APP_VERSION = "1.0.0";
    public static final String STORE_NAME = "NEXUS GAMING GEAR STORE";
    public static final String STORE_ADDRESS = "268 Lý Thường Kiệt, Quận 10, TP. Hồ Chí Minh";
    public static final String STORE_PHONE = "0909 888 999";

    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;
    public static final String DEFAULT_DATA_DIR = "data";
    public static final String DEFAULT_DATA_FILE = "gaming_gear_data.json";

    private int lowStockThreshold = DEFAULT_LOW_STOCK_THRESHOLD;
    private boolean autoSaveEnabled = true;
    private Path storagePath = Paths.get(DEFAULT_DATA_DIR, DEFAULT_DATA_FILE);

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        if (lowStockThreshold < 0) {
            throw new IllegalArgumentException("Ngưỡng cảnh báo tồn kho không được âm.");
        }
        this.lowStockThreshold = lowStockThreshold;
    }

    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled;
    }

    public void setAutoSaveEnabled(boolean autoSaveEnabled) {
        this.autoSaveEnabled = autoSaveEnabled;
    }

    public Path getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(Path storagePath) {
        if (storagePath == null) {
            throw new IllegalArgumentException("Đường dẫn tệp lưu trữ không được null.");
        }
        this.storagePath = storagePath;
    }
}
