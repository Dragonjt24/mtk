package com.gaminggear.patterns;

import com.gaminggear.exception.BusinessException;
import com.gaminggear.patterns.strategy.DiscountContext;
import com.gaminggear.patterns.strategy.NoDiscountStrategy;
import com.gaminggear.patterns.strategy.SeasonalDiscountStrategy;
import com.gaminggear.patterns.strategy.VIPDiscountStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DiscountStrategyTest {

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    @Test
    void noDiscountStrategy_calculatesZeroDiscountAndFullTotal() {
        DiscountContext context = new DiscountContext(new NoDiscountStrategy());
        assertEquals(0, new BigDecimal("0").compareTo(context.calculateDiscountAmount(ONE_MILLION)));
        assertEquals(0, new BigDecimal("1000000").compareTo(context.calculateFinalTotal(ONE_MILLION)));
    }

    @Test
    void vipDiscountStrategy_calculates15PercentDiscountAnd850kFinalTotal() {
        DiscountContext context = new DiscountContext(new VIPDiscountStrategy());
        assertEquals(0, new BigDecimal("150000").compareTo(context.calculateDiscountAmount(ONE_MILLION)));
        assertEquals(0, new BigDecimal("850000").compareTo(context.calculateFinalTotal(ONE_MILLION)));
    }

    @Test
    void seasonalDiscountStrategy_calculates20PercentDiscountAnd800kFinalTotal() {
        DiscountContext context = new DiscountContext(new SeasonalDiscountStrategy());
        assertEquals(0, new BigDecimal("200000").compareTo(context.calculateDiscountAmount(ONE_MILLION)));
        assertEquals(0, new BigDecimal("800000").compareTo(context.calculateFinalTotal(ONE_MILLION)));
    }

    @Test
    void discountContext_allowsDynamicStrategySwitchingAtRuntime() {
        DiscountContext context = new DiscountContext(new NoDiscountStrategy());
        assertEquals(0, new BigDecimal("1000000").compareTo(context.calculateFinalTotal(ONE_MILLION)));

        context.setStrategy(new VIPDiscountStrategy());
        assertEquals(0, new BigDecimal("850000").compareTo(context.calculateFinalTotal(ONE_MILLION)));

        context.setStrategy(new SeasonalDiscountStrategy());
        assertEquals(0, new BigDecimal("800000").compareTo(context.calculateFinalTotal(ONE_MILLION)));
    }

    @Test
    void negativeTotal_throwsBusinessException() {
        DiscountContext context = new DiscountContext(new VIPDiscountStrategy());
        assertThrows(BusinessException.class, () -> context.calculateDiscountAmount(new BigDecimal("-50000")));
        assertThrows(BusinessException.class, () -> context.setStrategy(null));
    }
}
