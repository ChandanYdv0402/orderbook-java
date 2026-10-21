package io.github.chandanydv0402.orderbook;

/**
 * A single order. Prices are whole ticks stored as {@code long}, never {@code double}.
 */
public final class Order {
    private final long id;
    private final Side side;
    private final long priceTicks;
    private long qty;
    private final long timestamp;

    public Order(long id, Side side, long priceTicks, long qty, long timestamp) {
        if (qty <= 0) {
            throw new IllegalArgumentException("qty must be positive: " + qty);
        }
        this.id = id;
        this.side = side;
        this.priceTicks = priceTicks;
        this.qty = qty;
        this.timestamp = timestamp;
    }

    public long id() { return id; }
    public Side side() { return side; }
    public long priceTicks() { return priceTicks; }
    public long qty() { return qty; }
    public long timestamp() { return timestamp; }
}
