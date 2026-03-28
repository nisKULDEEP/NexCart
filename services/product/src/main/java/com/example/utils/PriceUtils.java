package com.example.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for price-related calculations
 * Provides helper methods for handling monetary operations with precision
 */
public class PriceUtils {

    private static final int PRICE_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Formats price to 2 decimal places
     * @param price the price to format
     * @return formatted price
     */
    public static BigDecimal formatPrice(BigDecimal price) {
        if (price == null) {
            return BigDecimal.ZERO;
        }
        return price.setScale(PRICE_SCALE, ROUNDING_MODE);
    }

    /**
     * Calculates total price from unit price and quantity
     * @param unitPrice the price per unit
     * @param quantity the quantity
     * @return total price
     */
    public static BigDecimal calculateTotal(BigDecimal unitPrice, double quantity) {
        if (unitPrice == null || quantity <= 0) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(PRICE_SCALE, ROUNDING_MODE);
    }

    /**
     * Applies discount to a price
     * @param price the original price
     * @param discountPercentage the discount percentage (0-100)
     * @return discounted price
     */
    public static BigDecimal applyDiscount(BigDecimal price, double discountPercentage) {
        if (price == null || discountPercentage < 0 || discountPercentage > 100) {
            return price;
        }
        BigDecimal discount = price.multiply(BigDecimal.valueOf(discountPercentage / 100));
        return price.subtract(discount).setScale(PRICE_SCALE, ROUNDING_MODE);
    }

    /**
     * Checks if price is valid (positive and not null)
     * @param price the price to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPrice(BigDecimal price) {
        return price != null && price.compareTo(BigDecimal.ZERO) > 0;
    }
}

