package com.gaminggear.model.enums;

public enum ProductStatus {
    ACTIVE("Đang kinh doanh"),
    INACTIVE("Ngừng kinh doanh"),
    OUT_OF_STOCK("Hết hàng");

    private final String displayName;

    ProductStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
