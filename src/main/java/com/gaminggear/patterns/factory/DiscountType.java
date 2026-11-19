package com.gaminggear.patterns.factory;

/**
 * Supported discount policy types in the Gaming Gear store.
 */
public enum DiscountType {
    NO_DISCOUNT("Không giảm giá (0%)"),
    VIP("Khách hàng VIP (15%)"),
    SEASONAL("Khuyến mãi theo mùa / sự kiện (20%)");

    private final String displayName;

    DiscountType(String displayName) {
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
