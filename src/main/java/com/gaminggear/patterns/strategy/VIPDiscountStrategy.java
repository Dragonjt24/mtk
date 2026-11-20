package com.gaminggear.patterns.strategy;

import com.gaminggear.patterns.factory.DiscountType;
import com.gaminggear.util.ValidationUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete strategy applying 15% discount for VIP customers.
 */
public class VIPDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal DEFAULT_RATE = new BigDecimal("0.15");

    private final BigDecimal rate;

    public VIPDiscountStrategy() {
        this(DEFAULT_RATE);
    }

    public VIPDiscountStrategy(BigDecimal rate) {
        this.rate = (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) ? DEFAULT_RATE : rate;
    }

    @Override
    public DiscountType getDiscountType() {
        return DiscountType.VIP;
    }

    @Override
    public BigDecimal getDiscountRate() {
        return rate;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal originalTotal) {
        ValidationUtil.requireNonNegative(originalTotal, "Tổng tiền đơn hàng");
        return originalTotal.multiply(rate).setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public String getDescription() {
        int pct = rate.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValue();
        return "Ưu đãi thành viên VIP - Giảm " + pct + "% tổng đơn hàng";
    }
}
