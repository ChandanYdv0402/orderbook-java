package io.github.chandanydv0402.orderbook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {

    @Test
    void rejectsNonPositiveQty() {
        assertThrows(IllegalArgumentException.class, () -> new Order(1, Side.BUY, 10, 0, 0));
    }

    // TODO day one: write the 5 matching tests from docs/GAME_PLAN.md (Phase 0) before any engine code.
}
