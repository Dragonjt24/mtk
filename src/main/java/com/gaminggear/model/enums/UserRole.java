package com.gaminggear.model.enums;

public enum UserRole {
    ADMIN("Quản trị viên"),
    STAFF("Nhân viên bán hàng");

    private final String displayName;

    UserRole(String displayName) {
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
