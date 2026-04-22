package edu.classproject.order;

import java.util.List;

/**
 * OrderService — Team 9: Order Orchestration
 *
 * Validates restaurant/item references on order creation,
 * enforces status transition rules,
 * and handles payment success/failure paths.
 *
 * Dependencies (injected):
 *   - RestaurantValidator  (edu.classproject.restaurant)
 *   - PaymentGateway       (edu.classproject.payment)
 *   - NotificationService  (edu.classproject.notification)
 */
public class OrderService {

    private final RestaurantValidator restaurantValidator;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;

    public OrderService(RestaurantValidator restaurantValidator,
                        PaymentGateway paymentGateway,
                        NotificationService notificationService) {
        this.restaurantValidator = restaurantValidator;
        this.paymentGateway = paymentGateway;
        this.notificationService = notificationService;
    }

    // -------------------------------------------------------------------------
    // Order Creation
    // -------------------------------------------------------------------------

    /**
     * Creates a new order after validating the restaurant and each item reference.
     *
     * @throws IllegalArgumentException if the restaurant is inactive or any item is unavailable.
     */
    public Order createOrder(String customerId, String restaurantId, List<OrderItem> items) {
        // Validate restaurant
        if (!restaurantValidator.isRestaurantActive(restaurantId)) {
            throw new IllegalArgumentException(
                "Restaurant not found or inactive: " + restaurantId);
        }

        // Validate each item belongs to the restaurant
        for (OrderItem item : items) {
            if (!restaurantValidator.isItemAvailable(restaurantId, item.getItemId())) {
                throw new IllegalArgumentException(
                    "Item '" + item.getItemId() + "' is not available at restaurant: " + restaurantId);
            }
        }

        return new Order(customerId, restaurantId, items);
    }

    // -------------------------------------------------------------------------
    // Payment Processing
    // -------------------------------------------------------------------------

    /**
     * Processes payment for a given order.
     * On success: marks order CONFIRMED and notifies the customer.
     * On failure: marks order CANCELLED and notifies the customer.
     */
    public void processPayment(Order order) {
        boolean success = paymentGateway.processPayment(
            order.getOrderId(),
            order.getTotalAmount(),
            order.getCustomerId()
        );

        if (success) {
            order.markPaymentSuccess();
            notificationService.notifyOrderConfirmed(order.getCustomerId(), order.getOrderId());
        } else {
            order.markPaymentFailed();
            notificationService.notifyPaymentFailed(order.getCustomerId(), order.getOrderId());
        }
    }

    // -------------------------------------------------------------------------
    // Status Transitions
    // -------------------------------------------------------------------------

    /**
     * Advances an order to the next status.
     * Enforces transition rules — throws InvalidStatusTransitionException for illegal jumps.
     */
    public void advanceStatus(Order order, OrderStatus newStatus) {
        order.transitionTo(newStatus);
        notificationService.notifyOrderStatusUpdate(
            order.getCustomerId(), order.getOrderId(), newStatus);
    }
}
