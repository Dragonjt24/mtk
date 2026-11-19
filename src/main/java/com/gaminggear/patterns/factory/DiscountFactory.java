package com.gaminggear.patterns.factory;

import com.gaminggear.exception.BusinessException;
import com.gaminggear.patterns.strategy.DiscountStrategy;
import com.gaminggear.patterns.strategy.NoDiscountStrategy;
import com.gaminggear.patterns.strategy.SeasonalDiscountStrategy;
import com.gaminggear.patterns.strategy.VIPDiscountStrategy;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory Pattern responsible strictly for instantiating the appropriate DiscountStrategy
 * based on the selected DiscountType without a monolithic if/else block.
 */
public final class DiscountFactory {

    private static final Map<DiscountType, Supplier<DiscountStrategy>> STRATEGY_REGISTRY =
            new EnumMap<>(DiscountType.class);

    static {
        STRATEGY_REGISTRY.put(DiscountType.NO_DISCOUNT, NoDiscountStrategy::new);
        STRATEGY_REGISTRY.put(DiscountType.VIP, VIPDiscountStrategy::new);
        STRATEGY_REGISTRY.put(DiscountType.SEASONAL, SeasonalDiscountStrategy::new);
    }

    private DiscountFactory() {
    }

    public static DiscountStrategy createStrategy(DiscountType type) {
        if (type == null) {
            throw new BusinessException("Loại chính sách giảm giá không hợp lệ (null).");
        }
        Supplier<DiscountStrategy> supplier = STRATEGY_REGISTRY.get(type);
        if (supplier == null) {
            throw new BusinessException("Không hỗ trợ chính sách giảm giá: " + type);
        }
        return supplier.get();
    }

    public static DiscountStrategy createStrategy(String typeCode) {
        if (typeCode == null || typeCode.trim().isEmpty()) {
            throw new BusinessException("Mã chính sách giảm giá không được để trống.");
        }
        try {
            DiscountType parsed = DiscountType.valueOf(typeCode.trim().toUpperCase());
            return createStrategy(parsed);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Loại chính sách giảm giá không hợp lệ: " + typeCode, ex);
        }
    }
}
