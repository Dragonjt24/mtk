package com.gaminggear.util;

import com.gaminggear.exception.BusinessException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Shared validation helpers for business services and UI forms.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^0\\d{9,10}$");

    private ValidationUtil() {
    }

    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(fieldName + " không được để trống.");
        }
        return value.trim();
    }

    public static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new BusinessException(fieldName + " không được để trống.");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(fieldName + " không được là số âm.");
        }
        return value;
    }

    public static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        requireNonNegative(value, fieldName);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(fieldName + " phải lớn hơn 0.");
        }
        return value;
    }

    public static int requireNonNegativeInt(int value, String fieldName) {
        if (value < 0) {
            throw new BusinessException(fieldName + " không được là số âm.");
        }
        return value;
    }

    public static int requirePositiveInt(int value, String fieldName) {
        if (value <= 0) {
            throw new BusinessException(fieldName + " phải lớn hơn 0.");
        }
        return value;
    }

    public static String validateEmail(String email) {
        String trimmed = requireNonBlank(email, "Email");
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new BusinessException("Định dạng email không hợp lệ.");
        }
        return trimmed;
    }

    public static String validatePhone(String phone) {
        String trimmed = requireNonBlank(phone, "Số điện thoại");
        String normalized = trimmed.replaceAll("[\\s.-]", "");
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException("Số điện thoại phải bắt đầu bằng 0 và gồm 10-11 chữ số.");
        }
        return normalized;
    }
}
