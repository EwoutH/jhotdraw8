/*
 * @(#)IteratorEnumerator.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.collection.enumerator;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;

import java.util.Iterator;
import java.util.Spliterator;

/**
 * Wraps an {@link Iterator} into the {@link EnumeratorSpliterator} interface.
 *
 * @author Werner Randelshofer
 */
public class IteratorEnumeratorSpliterator<E> implements EnumeratorSpliterator<E> {
    private final @NonNull Iterator<? extends E> iterator;

    private E current;

    public IteratorEnumeratorSpliterator(final @NonNull Iterator<? extends E> iterator) {
        this.iterator = iterator;
    }


    @Override
    public boolean moveNext() {
        if (iterator.hasNext()) {
            current = iterator.next();
            return true;
        }
        return false;
    }

    @Override
    public E current() {
        return current;
    }

    @Override
    public @Nullable Spliterator<E> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return 0;
    }
}
