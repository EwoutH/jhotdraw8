/*
 * @(#)SeqChampSetTest.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.NonNull;

import java.util.Set;

public class SequencedChampSetTest extends AbstractSequencedSetTest {
    @Override
    protected <E> @NonNull SequencedSet<E> newInstance() {
        return new SequencedChampSet<>();
    }

    @Override
    protected <E> @NonNull SequencedSet<E> newInstance(int numElements, float loadFactor) {
        return new SequencedChampSet<>();
    }

    @Override
    protected <E> @NonNull SequencedSet<E> newInstance(Set<E> m) {
        return new SequencedChampSet<>(m);
    }

    @Override
    protected <E> @NonNull SequencedSet<E> newInstance(ReadOnlySet<E> m) {
        return new SequencedChampSet<>(m);
    }

    @Override
    protected <E> @NonNull ImmutableSequencedSet<E> toImmutableInstance(Set<E> m) {
        return new SequencedChampSet<>(m).toImmutable();
    }

    @Override
    protected <E> @NonNull SequencedSet<E> toClonedInstance(Set<E> m) {
        return ((SequencedChampSet<E>) m).clone();
    }

    @Override
    protected <E> @NonNull SequencedSet<E> newInstance(SequencedSet<E> m) {
        return new SequencedChampSet<>(m);
    }

    @Override
    protected <E> @NonNull SequencedSet<E> newInstance(ReadOnlySequencedSet<E> m) {
        return new SequencedChampSet<>(m);
    }

    @Override
    protected <E> @NonNull ImmutableSequencedSet<E> toImmutableInstance(SequencedSet<E> m) {
        return ((SequencedChampSet<E>) m).toImmutable();
    }

    @Override
    protected <E> @NonNull SequencedSet<E> toClonedInstance(SequencedSet<E> m) {
        return ((SequencedChampSet<E>) m).clone();
    }

    @Override
    protected <E> @NonNull SequencedSet<E> newInstance(Iterable<E> m) {
        return new SequencedChampSet<>(m);
    }
}
