package edu.classproject.order;

/**
 * Thrown when an illegal order status transition is attempted.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
