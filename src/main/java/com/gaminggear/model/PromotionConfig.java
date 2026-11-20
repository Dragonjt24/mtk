package com.gaminggear.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Global promotion configuration controlled by Admin.
 * When seasonalPromoEnabled is true, all accounts (VIP and NORMAL) automatically receive seasonal discount.
 * Otherwise VIP accounts receive VIP discount and NORMAL accounts receive NO_DISCOUNT.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PromotionConfig {

    private boolean seasonalPromoEnabled = false;
    private String seasonalPromoName = "Khuyến mãi sự kiện Gaming Festival";
    private BigDecimal seasonalDiscountRate = new BigDecimal("0.20");
    private BigDecimal vipDiscountRate = new BigDecimal("0.15");

    public PromotionConfig() {
    }

    public PromotionConfig(boolean seasonalPromoEnabled,
                           String seasonalPromoName,
                           BigDecimal seasonalDiscountRate,
                           BigDecimal vipDiscountRate) {
        this.seasonalPromoEnabled = seasonalPromoEnabled;
        this.seasonalPromoName = seasonalPromoName;
        this.seasonalDiscountRate = seasonalDiscountRate;
        this.vipDiscountRate = vipDiscountRate;
    }

    public boolean isSeasonalPromoEnabled() {
        return seasonalPromoEnabled;
    }

    public void setSeasonalPromoEnabled(boolean seasonalPromoEnabled) {
        this.seasonalPromoEnabled = seasonalPromoEnabled;
    }

    public String getSeasonalPromoName() {
        return seasonalPromoName;
    }

    public void setSeasonalPromoName(String seasonalPromoName) {
        this.seasonalPromoName = seasonalPromoName;
    }

    public BigDecimal getSeasonalDiscountRate() {
        return seasonalDiscountRate;
    }

    public void setSeasonalDiscountRate(BigDecimal seasonalDiscountRate) {
        this.seasonalDiscountRate = seasonalDiscountRate;
    }

    public BigDecimal getVipDiscountRate() {
        return vipDiscountRate;
    }

    public void setVipDiscountRate(BigDecimal vipDiscountRate) {
        this.vipDiscountRate = vipDiscountRate;
    }
}
