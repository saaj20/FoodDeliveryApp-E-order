package edu.classproject.order;

/**
 * Stub for the Restaurant module dependency.
 * In production, this would be injected via dependency injection.
 */
public interface RestaurantValidator {

    /**
     * Returns true if the restaurant exists and is currently active.
     */
    boolean isRestaurantActive(String restaurantId);

    /**
     * Returns true if the given item belongs to the given restaurant.
     */
    boolean isItemAvailable(String restaurantId, String itemId);
}
