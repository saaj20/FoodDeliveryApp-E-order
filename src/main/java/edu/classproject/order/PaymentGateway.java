package edu.classproject.order;

/**
 * Stub for the Payment module dependency.
 */
public interface PaymentGateway {

    /**
     * Processes payment for the given order.
     *
     * @return true if payment succeeded, false if it failed.
     */
    boolean processPayment(String orderId, double amount, String customerId);
}
