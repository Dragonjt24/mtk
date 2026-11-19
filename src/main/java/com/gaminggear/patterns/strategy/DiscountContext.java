package com.gaminggear.patterns.strategy;

import com.gaminggear.exception.BusinessException;
import com.gaminggear.util.ValidationUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Context class for the Strategy Pattern.
 * Allows switching discount calculation algorithms dynamically without modifying OrderService.
 */
public class DiscountContext {

    private DiscountStrategy strategy;

    public DiscountContext() {
        this.strategy = new NoDiscountStrategy();
    }

    public DiscountContext(DiscountStrategy strategy) {
        setStrategy(strategy);
    }

    public void setStrategy(DiscountStrategy strategy) {
        if (strategy == null) {
            throw new BusinessException("Chiến lược giảm giá không được null.");
        }
        this.strategy = strategy;
    }

    public DiscountStrategy getStrategy() {
        return strategy;
    }

    public BigDecimal calculateDiscountAmount(BigDecimal originalTotal) {
        ValidationUtil.requireNonNegative(originalTotal, "Tổng tiền gốc");
        BigDecimal discount = strategy.calculateDiscount(originalTotal);
        if (discount.compareTo(originalTotal) > 0) {
            return originalTotal.setScale(0, RoundingMode.HALF_UP);
        }
        return discount.setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateFinalTotal(BigDecimal originalTotal) {
        ValidationUtil.requireNonNegative(originalTotal, "Tổng tiền gốc");
        BigDecimal discount = calculateDiscountAmount(originalTotal);
        BigDecimal finalTotal = originalTotal.subtract(discount);
        return finalTotal.max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP);
    }
}
