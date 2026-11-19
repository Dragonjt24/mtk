package com.gaminggear.model.enums;

public enum PaymentMethod {
    COD("Thanh toán khi nhận hàng (COD)"),
    MOCK_SUCCESS("Thanh toán mô phỏng thành công"),
    MOCK_FAILURE("Thanh toán mô phỏng thất bại");

    private final String displayName;

    PaymentMethod(String displayName) {
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
