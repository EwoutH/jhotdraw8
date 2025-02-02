/*
 * @(#)SequencedEntry.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.collection.champ;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.collection.UniqueId;

import java.util.AbstractMap;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * A {@code SequencedEntry} stores an entry of a map and a sequence number.
 * <p>
 * {@code hashCode} and {@code equals} are based on the key and the value
 * of the entry - the sequence number is not included.
 */
class SequencedEntry<K, V> extends AbstractMap.SimpleImmutableEntry<K, V>
        implements SequencedData {
    private final static long serialVersionUID = 0L;
    private final int sequenceNumber;

    public SequencedEntry(@Nullable K key) {
        super(key, null);
        sequenceNumber = NO_SEQUENCE_NUMBER;
    }

    public SequencedEntry(@Nullable K key, @Nullable V value, int sequenceNumber) {
        super(key, value);
        this.sequenceNumber = sequenceNumber;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Renumbers the sequence numbers in all nodes from {@code 0} to {@code size}.
     * <p>
     * Afterwards the sequence number for the next inserted entry must be
     * set to the value {@code size};
     *
     * @param <K>     the key type
     * @param size
     * @param root    the root of the trie
     * @param mutator the mutator which will own all nodes of the trie
     * @return the new root
     */
    public static <K, V> BitmapIndexedNode<SequencedEntry<K, V>> renumber(int size, @NonNull BitmapIndexedNode<SequencedEntry<K, V>> root, @NonNull UniqueId mutator,
                                                                          @NonNull ToIntFunction<SequencedEntry<K, V>> hashFunction,
                                                                          @NonNull BiPredicate<SequencedEntry<K, V>, SequencedEntry<K, V>> equalsFunction) {
        if (size == 0) {
            return root;
        }

        BitmapIndexedNode<SequencedEntry<K, V>> newRoot = root;
        ChangeEvent<SequencedEntry<K, V>> details = new ChangeEvent<>();
        int seq = 0;
        BiFunction<SequencedEntry<K, V>, SequencedEntry<K, V>, SequencedEntry<K, V>> updateFunction = (oldk, newk) -> oldk.getSequenceNumber() == newk.getSequenceNumber() ? oldk : newk;
        for (HeapSequencedIterator<SequencedEntry<K, V>, SequencedEntry<K, V>> i = new HeapSequencedIterator<>(size, root, false, null, Function.identity()); i.hasNext(); ) {
            SequencedEntry<K, V> e = i.next();
            SequencedEntry<K, V> newElement = new SequencedEntry<>(e.getKey(), e.getValue(), seq);
            newRoot = newRoot.update(mutator,
                    newElement,
                    Objects.hashCode(e.getKey()), 0, details,
                    updateFunction,
                    equalsFunction, hashFunction);
            seq++;
        }
        return newRoot;
    }

}
