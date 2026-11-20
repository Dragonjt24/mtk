package com.gaminggear.patterns.strategy;

import com.gaminggear.patterns.factory.DiscountType;
import com.gaminggear.util.ValidationUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete strategy applying 20% discount for seasonal / promotional events.
 */
public class SeasonalDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal DEFAULT_RATE = new BigDecimal("0.20");
    private static final String DEFAULT_EVENT_NAME = "Khuyến mãi sự kiện Gaming Festival";

    private final BigDecimal rate;
    private final String eventName;

    public SeasonalDiscountStrategy() {
        this(DEFAULT_RATE, DEFAULT_EVENT_NAME);
    }

    public SeasonalDiscountStrategy(BigDecimal rate, String eventName) {
        this.rate = (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) ? DEFAULT_RATE : rate;
        this.eventName = (eventName == null || eventName.isBlank()) ? DEFAULT_EVENT_NAME : eventName.trim();
    }

    @Override
    public DiscountType getDiscountType() {
        return DiscountType.SEASONAL;
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
        return eventName + " - Giảm " + pct + "% tổng đơn hàng (áp dụng cho toàn bộ tài khoản)";
    }
}
