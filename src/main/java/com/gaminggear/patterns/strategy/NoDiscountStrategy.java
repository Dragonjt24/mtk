package com.gaminggear.patterns.strategy;

import com.gaminggear.patterns.factory.DiscountType;
import com.gaminggear.util.ValidationUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete strategy applying 0% discount.
 */
public class NoDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal RATE = new BigDecimal("0.00");

    @Override
    public DiscountType getDiscountType() {
        return DiscountType.NO_DISCOUNT;
    }

    @Override
    public BigDecimal getDiscountRate() {
        return RATE;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal originalTotal) {
        ValidationUtil.requireNonNegative(originalTotal, "Tổng tiền đơn hàng");
        return BigDecimal.ZERO.setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public String getDescription() {
        return "Không áp dụng giảm giá (0%)";
    }
}
