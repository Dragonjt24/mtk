package com.gaminggear.patterns.strategy;

import com.gaminggear.patterns.factory.DiscountType;
import com.gaminggear.util.ValidationUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete strategy applying 20% discount for seasonal / promotional events.
 */
public class SeasonalDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal RATE = new BigDecimal("0.20");

    @Override
    public DiscountType getDiscountType() {
        return DiscountType.SEASONAL;
    }

    @Override
    public BigDecimal getDiscountRate() {
        return RATE;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal originalTotal) {
        ValidationUtil.requireNonNegative(originalTotal, "Tổng tiền đơn hàng");
        return originalTotal.multiply(RATE).setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public String getDescription() {
        return "Khuyến mãi sự kiện Gaming Festival - Giảm 20% tổng đơn hàng";
    }
}
