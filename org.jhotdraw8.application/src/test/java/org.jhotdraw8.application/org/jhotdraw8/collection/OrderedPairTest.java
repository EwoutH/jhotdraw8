package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderedPairTest {
    @Test
    public void testNonNull() {
        OrderedPair<@NonNull Integer, @NonNull Integer> op = new OrderedPair<>(3, 5);
        int sum = op.first() + op.second();
        assertEquals(8, sum);
    }

    @Test
    public void testNullable() {
        OrderedPair<@Nullable Integer, @Nullable Integer> op = new OrderedPair<>(3, 5);
        int sum = op.first() + op.second();
        assertEquals(8, sum);
    }
}