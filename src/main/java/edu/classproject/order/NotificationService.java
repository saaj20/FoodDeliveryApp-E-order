package edu.classproject.order;

/**
 * Stub for the Notification module dependency.
 */
public interface NotificationService {

    void notifyOrderConfirmed(String customerId, String orderId);

    void notifyPaymentFailed(String customerId, String orderId);

    void notifyOrderStatusUpdate(String customerId, String orderId, OrderStatus newStatus);
}
