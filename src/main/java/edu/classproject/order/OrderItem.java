package edu.classproject.order;

/**
 * Represents a single item in an order.
 */
public class OrderItem {

    private final String itemId;
    private final String name;
    private final int quantity;
    private final double unitPrice;

    public OrderItem(String itemId, String name, int quantity, double unitPrice) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item ID must not be blank.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Item name must not be blank.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException("Unit price must not be negative.");
        }
        this.itemId = itemId;
        this.name = name;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getSubtotal() { return quantity * unitPrice; }

    @Override
    public String toString() {
        return String.format("OrderItem{itemId='%s', name='%s', qty=%d, unitPrice=%.2f}",
                itemId, name, quantity, unitPrice);
    }
}
