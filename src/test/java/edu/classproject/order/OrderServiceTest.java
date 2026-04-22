package edu.classproject.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Team 9 — Order Orchestration
 *
 * Common rules compliance:
 *  - Happy path tests for all 3 requirements
 *  - At least 2 failure/edge cases per area
 *  - Interface contract safety (unmodifiable collections, unique IDs)
 */
class OrderServiceTest {

    private static class FakeRestaurantValidator implements RestaurantValidator {
        private final boolean restaurantActive;
        private final boolean itemAvailable;
        FakeRestaurantValidator(boolean restaurantActive, boolean itemAvailable) {
            this.restaurantActive = restaurantActive;
            this.itemAvailable = itemAvailable;
        }
        @Override public boolean isRestaurantActive(String restaurantId) { return restaurantActive; }
        @Override public boolean isItemAvailable(String restaurantId, String itemId) { return itemAvailable; }
    }

    private static class FakePaymentGateway implements PaymentGateway {
        private final boolean shouldSucceed;
        FakePaymentGateway(boolean shouldSucceed) { this.shouldSucceed = shouldSucceed; }
        @Override public boolean processPayment(String orderId, double amount, String customerId) { return shouldSucceed; }
    }

    private static class FakeNotificationService implements NotificationService {
        String lastEvent = "";
        @Override public void notifyOrderConfirmed(String customerId, String orderId) { lastEvent = "CONFIRMED:" + orderId; }
        @Override public void notifyPaymentFailed(String customerId, String orderId) { lastEvent = "PAYMENT_FAILED:" + orderId; }
        @Override public void notifyOrderStatusUpdate(String customerId, String orderId, OrderStatus newStatus) { lastEvent = "STATUS_UPDATE:" + newStatus; }
    }

    private static final String CUSTOMER_ID   = "cust-001";
    private static final String RESTAURANT_ID = "rest-101";
    private static final List<OrderItem> VALID_ITEMS = List.of(
        new OrderItem("item-A", "Biryani", 2, 180.0),
        new OrderItem("item-B", "Raita",   1,  30.0)
    );

    private FakeNotificationService notifier;

    @BeforeEach
    void setUp() { notifier = new FakeNotificationService(); }

    // =========================================================================
    // 1. Order Creation Validation
    // =========================================================================

    @Nested
    @DisplayName("Order creation validates restaurant and item references")
    class OrderCreationTests {

        @Test
        @DisplayName("Happy path: creates order with valid restaurant and items")
        void createOrder_validRestaurantAndItems_succeeds() {
            OrderService service = new OrderService(new FakeRestaurantValidator(true, true), new FakePaymentGateway(true), notifier);
            Order order = service.createOrder(CUSTOMER_ID, RESTAURANT_ID, VALID_ITEMS);
            assertNotNull(order);
            assertEquals(RESTAURANT_ID, order.getRestaurantId());
            assertEquals(CUSTOMER_ID, order.getCustomerId());
            assertEquals(OrderStatus.PENDING, order.getStatus());
            assertEquals(390.0, order.getTotalAmount(), 0.001);
        }

        @Test
        @DisplayName("Failure: inactive restaurant is rejected")
        void createOrder_inactiveRestaurant_throwsException() {
            OrderService service = new OrderService(new FakeRestaurantValidator(false, true), new FakePaymentGateway(true), notifier);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createOrder(CUSTOMER_ID, RESTAURANT_ID, VALID_ITEMS));
            assertTrue(ex.getMessage().contains("Restaurant not found or inactive"));
        }

        @Test
        @DisplayName("Failure: unavailable item is rejected")
        void createOrder_unavailableItem_throwsException() {
            OrderService service = new OrderService(new FakeRestaurantValidator(true, false), new FakePaymentGateway(true), notifier);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createOrder(CUSTOMER_ID, RESTAURANT_ID, VALID_ITEMS));
            assertTrue(ex.getMessage().contains("not available at restaurant"));
        }

        @Test
        @DisplayName("Edge case: empty items list is rejected")
        void createOrder_emptyItems_throwsException() {
            OrderService service = new OrderService(new FakeRestaurantValidator(true, true), new FakePaymentGateway(true), notifier);
            assertThrows(IllegalArgumentException.class,
                () -> service.createOrder(CUSTOMER_ID, RESTAURANT_ID, List.of()));
        }

        @Test
        @DisplayName("Edge case: null customerId is rejected")
        void createOrder_nullCustomerId_throwsException() {
            OrderService service = new OrderService(new FakeRestaurantValidator(true, true), new FakePaymentGateway(true), notifier);
            assertThrows(IllegalArgumentException.class,
                () -> service.createOrder(null, RESTAURANT_ID, VALID_ITEMS));
        }

        @Test
        @DisplayName("Edge case: blank restaurantId is rejected")
        void createOrder_blankRestaurantId_throwsException() {
            OrderService service = new OrderService(new FakeRestaurantValidator(true, true), new FakePaymentGateway(true), notifier);
            assertThrows(IllegalArgumentException.class,
                () -> service.createOrder(CUSTOMER_ID, "   ", VALID_ITEMS));
        }
    }

    // =========================================================================
    // 2. Status Transition Rules
    // =========================================================================

    @Nested
    @DisplayName("Status transition rules are enforced (no arbitrary jumps)")
    class StatusTransitionTests {

        private Order paidOrder() {
            OrderService service = new OrderService(new FakeRestaurantValidator(true, true), new FakePaymentGateway(true), notifier);
            Order order = service.createOrder(CUSTOMER_ID, RESTAURANT_ID, VALID_ITEMS);
            service.processPayment(order);
            return order;
        }

        @Test
        @DisplayName("Happy path: full valid transition CONFIRMED -> DELIVERED")
        void validTransitionPath_succeeds() {
            OrderService service = new OrderService(new FakeRestaurantValidator(true, true), new FakePaymentGateway(true), notifier);
            Order order = paidOrder();
            assertEquals(OrderStatus.CONFIRMED, order.getStatus());
            service.advanceStatus(order, OrderStatus.PREPARING);
            service.advanceStatus(order, OrderStatus.READY_FOR_PICKUP);
            service.advanceStatus(order, OrderStatus.OUT_FOR_DELIVERY);
            service.advanceStatus(order, OrderStatus.DELIVERED);
            assertEquals(OrderStatus.DELIVERED, order.getStatus());
        }

        @Test
        @DisplayName("Failure: arbitrary jump CONFIRMED -> DELIVERED is blocked")
        void illegalTransition_confirmedToDelivered_throwsException() {
            Order order = paidOrder();
            assertThrows(InvalidStatusTransitionException.class,
                () -> order.transitionTo(OrderStatus.DELIVERED));
        }

        @Test
        @DisplayName("Failure: jump PENDING -> PREPARING is blocked")
        void illegalTransition_pendingToPreparing_throwsException() {
            OrderService service = new OrderService(new FakeRestaurantValidator(true, true), new FakePaymentGateway(true), notifier);
            Order order = service.createOrder(CUSTOMER_ID, RESTAURANT_ID, VALID_ITEMS);
            assertThrows(InvalidStatusTransitionException.class,
                () -> order.transitionTo(OrderStatus.PREPARING));
        }

        @Test
        @DisplayName("Edge case: no transition allowed from terminal DELIVERED")
        void illegalTransition_fromDelivered_throwsException() {
            Order order = paidOrder();
            order.transitionTo(OrderStatus.PREPARING);
            order.transitionTo(OrderStatus.READY_FOR_PICKUP);
            order.transitionTo(OrderStatus.OUT_FOR_DELIVERY);
            order.transitionTo(OrderStatus.DELIVERED);
            assertThrows(InvalidStatusTransitionException.class,
                () -> order.transitionTo(OrderStatus.CONFIRMED));
        }

        @Test
        @DisplayName("Edge case: no transition allowed from terminal CANCELLED")
        void illegalTransition_fromCancelled_throwsException() {
            OrderService service = new OrderService(new FakeRestaurantValidator(true, true), new FakePaymentGateway(false), notifier);
            Order order = service.createOrder(CUSTOMER_ID, RESTAURANT_ID, VALID_ITEMS);
            service.processPayment(order);
            assertThrows(InvalidStatusTransitionException.class,
                () -> order.transitionTo(OrderStatus.PENDING));
        }
    }

    // =========================================================================
    // 3. Payment Paths
    // =========================================================================

    @Nested
    @DisplayName("Payment success and failure paths are both tested")
    class PaymentTests {

        @Test
        @DisplayName("Happy path: payment success -> CONFIRMED, PAID, notification sent")
        void paymentSuccess_confirmsOrderAndNotifies() {
            OrderService service = new OrderService(new FakeRestaurantValidator(true, true), new FakePaymentGateway(true), notifier);
            Order order = service.createOrder(CUSTOMER_ID, RESTAURANT_ID, VALID_ITEMS);
            service.processPayment(order);
            assertEquals(OrderStatus.CONFIRMED, order.getStatus());
            assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
            assertTrue(notifier.lastEvent.startsWith("CONFIRMED:"));
        }

        @Test
        @DisplayName("Failure: payment failure -> CANCELLED, FAILED, notification sent")
        void paymentFailure_cancelsOrderAndNotifies() {
            OrderService service = new OrderService(new FakeRestaurantValidator(true, true), new FakePaymentGateway(false), notifier);
            Order order = service.createOrder(CUSTOMER_ID, RESTAURANT_ID, VALID_ITEMS);
            service.processPayment(order);
            assertEquals(OrderStatus.CANCELLED, order.getStatus());
            assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
            assertTrue(notifier.lastEvent.startsWith("PAYMENT_FAILED:"));
        }

        @Test
        @DisplayName("Edge case: correct total amount is sent to payment gateway")
        void paymentAmount_matchesOrderTotal() {
            final double[] capturedAmount = {0.0};
            PaymentGateway capturingGateway = (orderId, amount, customerId) -> { capturedAmount[0] = amount; return true; };
            OrderService service = new OrderService(new FakeRestaurantValidator(true, true), capturingGateway, notifier);
            Order order = service.createOrder(CUSTOMER_ID, RESTAURANT_ID, VALID_ITEMS);
            service.processPayment(order);
            assertEquals(390.0, capturedAmount[0], 0.001);
        }
    }

    // =========================================================================
    // 4. Interface Contract Safety (Common Rules)
    // =========================================================================

    @Nested
    @DisplayName("Interface contract and data integrity checks")
    class InterfaceContractTests {

        @Test
        @DisplayName("Items list from Order is unmodifiable — protects internal state")
        void order_itemsList_isUnmodifiable() {
            OrderService service = new OrderService(new FakeRestaurantValidator(true, true), new FakePaymentGateway(true), notifier);
            Order order = service.createOrder(CUSTOMER_ID, RESTAURANT_ID, VALID_ITEMS);
            assertThrows(UnsupportedOperationException.class,
                () -> order.getItems().add(new OrderItem("item-Z", "Extra", 1, 50.0)));
        }

        @Test
        @DisplayName("New order always starts with PENDING and UNPAID")
        void newOrder_hasCorrectInitialState() {
            OrderService service = new OrderService(new FakeRestaurantValidator(true, true), new FakePaymentGateway(true), notifier);
            Order order = service.createOrder(CUSTOMER_ID, RESTAURANT_ID, VALID_ITEMS);
            assertEquals(OrderStatus.PENDING, order.getStatus());
            assertEquals(PaymentStatus.UNPAID, order.getPaymentStatus());
            assertNotNull(order.getOrderId());
            assertNotNull(order.getCreatedAt());
        }

        @Test
        @DisplayName("Each order gets a unique ID — no collisions")
        void createOrder_twoOrders_haveDistinctIds() {
            OrderService service = new OrderService(new FakeRestaurantValidator(true, true), new FakePaymentGateway(true), notifier);
            Order o1 = service.createOrder(CUSTOMER_ID, RESTAURANT_ID, VALID_ITEMS);
            Order o2 = service.createOrder(CUSTOMER_ID, RESTAURANT_ID, VALID_ITEMS);
            assertNotEquals(o1.getOrderId(), o2.getOrderId());
        }

        @Test
        @DisplayName("OrderItem with zero quantity is rejected")
        void orderItem_zeroQuantity_throwsException() {
            assertThrows(IllegalArgumentException.class,
                () -> new OrderItem("item-X", "Dosa", 0, 60.0));
        }

        @Test
        @DisplayName("OrderItem with negative price is rejected")
        void orderItem_negativePrice_throwsException() {
            assertThrows(IllegalArgumentException.class,
                () -> new OrderItem("item-X", "Dosa", 1, -10.0));
        }
    }
}
