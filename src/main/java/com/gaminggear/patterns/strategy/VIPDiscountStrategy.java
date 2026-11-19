package com.gaminggear.patterns.strategy;

import com.gaminggear.patterns.factory.DiscountType;
import com.gaminggear.util.ValidationUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete strategy applying 15% discount for VIP customers.
 */
public class VIPDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal RATE = new BigDecimal("0.15");

    @Override
    public DiscountType getDiscountType() {
        return DiscountType.VIP;
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
        return "Ưu đãi thành viên VIP - Giảm 15% tổng đơn hàng";
    }
}
