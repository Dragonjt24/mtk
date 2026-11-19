package com.gaminggear.patterns;

import com.gaminggear.exception.BusinessException;
import com.gaminggear.patterns.factory.DiscountFactory;
import com.gaminggear.patterns.factory.DiscountType;
import com.gaminggear.patterns.strategy.DiscountStrategy;
import com.gaminggear.patterns.strategy.NoDiscountStrategy;
import com.gaminggear.patterns.strategy.SeasonalDiscountStrategy;
import com.gaminggear.patterns.strategy.VIPDiscountStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiscountFactoryTest {

    @Test
    void createNoDiscountStrategy_returnsNoDiscountStrategy() {
        DiscountStrategy strategy = DiscountFactory.createStrategy(DiscountType.NO_DISCOUNT);
        assertNotNull(strategy);
        assertInstanceOf(NoDiscountStrategy.class, strategy);
        assertEquals(DiscountType.NO_DISCOUNT, strategy.getDiscountType());
    }

    @Test
    void createVipStrategy_returnsVipDiscountStrategy() {
        DiscountStrategy strategy = DiscountFactory.createStrategy(DiscountType.VIP);
        assertNotNull(strategy);
        assertInstanceOf(VIPDiscountStrategy.class, strategy);
        assertEquals(DiscountType.VIP, strategy.getDiscountType());
    }

    @Test
    void createSeasonalStrategy_returnsSeasonalDiscountStrategy() {
        DiscountStrategy strategy = DiscountFactory.createStrategy(DiscountType.SEASONAL);
        assertNotNull(strategy);
        assertInstanceOf(SeasonalDiscountStrategy.class, strategy);
        assertEquals(DiscountType.SEASONAL, strategy.getDiscountType());
    }

    @Test
    void createStrategy_withNullOrInvalidType_throwsBusinessException() {
        assertThrows(BusinessException.class, () -> DiscountFactory.createStrategy((DiscountType) null));
        assertThrows(BusinessException.class, () -> DiscountFactory.createStrategy("UNKNOWN_POLICY"));
        assertThrows(BusinessException.class, () -> DiscountFactory.createStrategy("   "));
    }
}
