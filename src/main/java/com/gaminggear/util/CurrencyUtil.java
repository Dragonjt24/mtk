package com.gaminggear.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Currency formatting and BigDecimal rounding utilities (VND).
 */
public final class CurrencyUtil {

    private static final DecimalFormat VND_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        VND_FORMAT = new DecimalFormat("#,##0", symbols);
    }

    private CurrencyUtil() {
    }

    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(0, RoundingMode.HALF_UP);
        }
        return amount.setScale(0, RoundingMode.HALF_UP);
    }

    public static String formatVnd(BigDecimal amount) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount;
        return VND_FORMAT.format(safe.setScale(0, RoundingMode.HALF_UP)) + " VNĐ";
    }

    public static String formatNumber(BigDecimal amount) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount;
        return VND_FORMAT.format(safe.setScale(0, RoundingMode.HALF_UP));
    }

    public static String formatPercent(BigDecimal rate) {
        if (rate == null) {
            return "0%";
        }
        BigDecimal percent = rate.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);
        return percent.toPlainString() + "%";
    }
}
