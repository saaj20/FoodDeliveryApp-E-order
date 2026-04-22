package edu.classproject.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Core Order entity for the Order Orchestration module.
 *
 * Responsibilities:
 *  - Holds order data (restaurantId, customerId, items)
 *  - Enforces status transition rules
 *  - Tracks payment status
 */
public class Order {

    private final String orderId;
    private final String customerId;
    private final String restaurantId;
    private final List<OrderItem> items;
    private final LocalDateTime createdAt;

    private OrderStatus status;
    private PaymentStatus paymentStatus;

    public Order(String customerId, String restaurantId, List<OrderItem> items) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID must not be blank.");
        }
        if (restaurantId == null || restaurantId.isBlank()) {
            throw new IllegalArgumentException("Restaurant ID must not be blank.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item.");
        }

        this.orderId = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.items = new ArrayList<>(items);
        this.createdAt = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
        this.paymentStatus = PaymentStatus.UNPAID;
    }

    /**
     * Transitions the order to a new status.
     * Throws InvalidStatusTransitionException if the transition is not allowed.
     */
    public void transitionTo(OrderStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(
                String.format("Cannot transition from %s to %s.", this.status, newStatus)
            );
        }
        this.status = newStatus;
    }

    /**
     * Marks payment as successful and confirms the order.
     */
    public void markPaymentSuccess() {
        this.paymentStatus = PaymentStatus.PAID;
        transitionTo(OrderStatus.CONFIRMED);
    }

    /**
     * Marks payment as failed and cancels the order.
     */
    public void markPaymentFailed() {
        this.paymentStatus = PaymentStatus.FAILED;
        transitionTo(OrderStatus.CANCELLED);
    }

    public double getTotalAmount() {
        return items.stream().mapToDouble(OrderItem::getSubtotal).sum();
    }

    // --- Getters ---

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getRestaurantId() { return restaurantId; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public OrderStatus getStatus() { return status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }

    @Override
    public String toString() {
        return String.format(
            "Order{orderId='%s', restaurantId='%s', customerId='%s', status=%s, payment=%s, total=%.2f}",
            orderId, restaurantId, customerId, status, paymentStatus, getTotalAmount()
        );
    }
}
