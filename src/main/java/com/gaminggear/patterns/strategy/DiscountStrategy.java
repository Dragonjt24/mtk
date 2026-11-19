package com.gaminggear.patterns.strategy;

import com.gaminggear.patterns.factory.DiscountType;

import java.math.BigDecimal;

/**
 * Strategy interface defining the contract for discount calculation algorithms.
 */
public interface DiscountStrategy {

    DiscountType getDiscountType();

    BigDecimal getDiscountRate();

    BigDecimal calculateDiscount(BigDecimal originalTotal);

    String getDescription();
}
