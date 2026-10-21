package io.github.chandanydv0402.orderbook;

/**
 * Everything the engine emits. Tests assert on the event stream.
 */
public sealed interface Event {
    record Accepted(long orderId) implements Event {}
    record Trade(long aggressorId, long restingId, long priceTicks, long qty) implements Event {}
    record Cancelled(long orderId, long remainingQty) implements Event {}
}
