package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.NonNull;

import java.util.Arrays;
import java.util.Set;

public class Sets {

    /**
     * Don't let anyone instantiate this class.
     */
    private Sets() {
    }

    public static <E> Set<E> addAll(@NonNull Set<E> set, E... elements) {
        set.addAll(Arrays.asList(elements));
        return set;
    }
}
