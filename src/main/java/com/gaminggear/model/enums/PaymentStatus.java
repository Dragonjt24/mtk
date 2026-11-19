package com.gaminggear.model.enums;

public enum PaymentStatus {
    SUCCESS("Thành công"),
    FAILED("Thất bại");

    private final String displayName;

    PaymentStatus(String displayName) {
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
