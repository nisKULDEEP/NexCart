package com.example.utils;

/**
 * Utility class for quantity validations and calculations
 */
public class QuantityUtils {

    /**
     * Validates if quantity is positive
     * @param quantity the quantity to validate
     * @return true if quantity is positive
     */
    public static boolean isPositiveQuantity(double quantity) {
        return quantity > 0;
    }

    /**
     * Validates if there's sufficient stock
     * @param availableQuantity available stock
     * @param requestedQuantity requested quantity
     * @return true if sufficient stock available
     */
    public static boolean hasSufficientStock(double availableQuantity, double requestedQuantity) {
        return availableQuantity >= requestedQuantity;
    }

    /**
     * Calculates remaining quantity after purchase
     * @param availableQuantity available stock
     * @param purchasedQuantity quantity purchased
     * @return remaining quantity
     */
    public static double calculateRemainingQuantity(double availableQuantity, double purchasedQuantity) {
        double remaining = availableQuantity - purchasedQuantity;
        return remaining < 0 ? 0 : remaining;
    }

    /**
     * Checks if stock is low (less than 10 units)
     * @param quantity the stock quantity
     * @return true if stock is low
     */
    public static boolean isLowStock(double quantity) {
        return quantity < 10;
    }
}

