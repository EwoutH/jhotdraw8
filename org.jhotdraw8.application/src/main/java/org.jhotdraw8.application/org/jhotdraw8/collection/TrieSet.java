/*
 * @(#)TrieSet.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.collection.ChampTrie.BitmapIndexedNode;
import org.jhotdraw8.collection.ChampTrie.ChangeEvent;
import org.jhotdraw8.collection.ChampTrie.KeyIterator;

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * Implements a mutable set using a Compressed Hash-Array Mapped Prefix-tree
 * (CHAMP).
 * <p>
 * Features:
 * <ul>
 *     <li>allows null elements</li>
 *     <li>is mutable</li>
 *     <li>is not thread-safe</li>
 *     <li>does not guarantee a specific iteration order</li>
 * </ul>
 * <p>
 * Performance characteristics:
 * <ul>
 *     <li>add: O(1)</li>
 *     <li>remove: O(1)</li>
 *     <li>contains: O(1)</li>
 *     <li>toPersistent: O(log n) distributed across subsequent updates</li>
 *     <li>clone: O(log n) distributed across subsequent updates</li>
 *     <li>iterator.next(): O(1)</li>
 * </ul>
 * <p>
 * Implementation details:
 * <p>
 * This set performs read and write operations of single elements in O(1) time,
 * and in O(1) space.
 * <p>
 * The CHAMP tree contains nodes that may be shared with other sets, and nodes
 * that are exclusively owned by this set.
 * <p>
 * If a write operation is performed on an exclusively owned node, then this
 * set is allowed to mutate the node (mutate-on-write).
 * If a write operation is performed on a potentially shared node, then this
 * set is forced to create an exclusive copy of the node and of all not (yet)
 * exclusively owned parent nodes up to the root (copy-path-on-write).
 * Since the CHAMP tree has a fixed maximal height, the cost is O(1) in either
 * case.
 * <p>
 * This set can create a persistent copy of itself in O(1) time and O(0) space
 * using method {@link #toPersistent()}. This set loses exclusive ownership of
 * all its tree nodes.
 * Thus, creating a persistent copy increases the constant cost of
 * subsequent writes, until all shared nodes have been gradually replaced by
 * exclusively owned nodes again.
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access this set concurrently, and at least
 * one of the threads modifies the set, it <em>must</em> be synchronized
 * externally.  This is typically accomplished by synchronizing on some
 * object that naturally encapsulates the set.
 * <p>
 * References:
 * <dl>
 *      <dt>Michael J. Steindorfer (2017).
 *      Efficient Immutable Collections.</dt>
 *      <dd><a href="https://michael.steindorfer.name/publications/phd-thesis-efficient-immutable-collections">michael.steindorfer.name</a>
 *
 *      <dt>The Capsule Hash Trie Collections Library.
 *      <br>Copyright (c) Michael Steindorfer. BSD-2-Clause License</dt>
 *      <dd><a href="https://github.com/usethesource/capsule">github.com</a>
 * </dl>
 *
 * @param <E> the element type
 */
public class TrieSet<E> extends AbstractSet<E> implements Serializable, Cloneable {
    private final static long serialVersionUID = 0L;
    private final static int TUPLE_LENGTH = 1;
    private transient UniqueIdentity mutator;
    private BitmapIndexedNode<E, Void> root;
    private int size;
    private int modCount;
    private final static ToIntFunction<Object> hashFunction = Objects::hashCode;

    /**
     * Constructs an empty set.
     */
    public TrieSet() {
        this.root = ChampTrie.emptyNode();
    }

    /**
     * Constructs a set containing the elements in the specified iterable.
     *
     * @param c an iterable
     */
    @SuppressWarnings("unchecked")
    public TrieSet(Iterable<? extends E> c) {
        if (c instanceof TrieSet<?>) {
            c = ((TrieSet<? extends E>) c).toPersistent();
        }
        if (c instanceof PersistentTrieSet<?>) {
            PersistentTrieSet<E> that = (PersistentTrieSet<E>) c;
            this.root = that;
            this.size = that.size;
        } else {
            this.root = ChampTrie.emptyNode();
            addAll(c);
        }
    }

    /**
     * Adds the specified element if it is not already in this set.
     *
     * @param e an element
     * @return {@code true} if this set changed
     */
    public boolean add(final @Nullable E e) {
        final ChangeEvent<Void> changeEvent = new ChangeEvent<>();
        final BitmapIndexedNode<E, Void> newRoot = root.update(getOrCreateMutator(), e, null, hash(e), 0, changeEvent, TUPLE_LENGTH,
                this::hash, ChampTrie.TUPLE_VALUE);
        if (changeEvent.isModified) {
            root = newRoot;
            size++;
            modCount++;
            return true;
        }
        return false;
    }

    /**
     * Computes a hash code for the specified object.
     *
     * @param e an object
     * @return hash code
     */
    private int hash(@Nullable E e) {
        return hashFunction.applyAsInt(e);
    }

    /**
     * Adds all specified elements that are not already in this set.
     *
     * @param c a collection of elements
     * @return {@code true} if this set changed
     */
    @Override
    public boolean addAll(@NonNull Collection<? extends E> c) {
        return addAll((Iterable<? extends E>) c);
    }

    /**
     * Adds all specified elements that are not already in this set.
     *
     * @param c an iterable of elements
     * @return {@code true} if this set changed
     */
    public boolean addAll(@NonNull Iterable<? extends E> c) {
        if (c == this) {
            return false;
        }

        boolean modified = false;
        for (E e : c) {
            modified |= add(e);
        }
        return modified;
    }

    /**
     * Removes all elements from this set.
     */
    @Override
    public void clear() {
        root = ChampTrie.emptyNode();
        size = 0;
        modCount++;
    }

    /**
     * Returns a shallow copy of this set.
     */
    @Override
    public TrieSet<E> clone() {
        try {
            @SuppressWarnings("unchecked") final TrieSet<E> that = (TrieSet<E>) super.clone();
            that.mutator = null;
            this.mutator = null;
            return that;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(@Nullable final Object o) {
        @SuppressWarnings("unchecked") final E key = (E) o;
        return root.containsKey(key, hash(key), 0, TUPLE_LENGTH);
    }

    private @NonNull UniqueIdentity getOrCreateMutator() {
        if (mutator == null) {
            mutator = new UniqueIdentity();
        }
        return mutator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns an iterator over the elements of this set.
     */
    @Override
    public Iterator<E> iterator() {
        return new MutableTrieIterator<>(this, TUPLE_LENGTH, this::hash);
    }

    /**
     * Removes the specified element if it is in this set.
     *
     * @param o an element
     * @return {@code true} if this set changed
     */
    public boolean remove(final Object o) {
        @SuppressWarnings("unchecked")
        E key = (E) o;
        final ChangeEvent<Void> changeEvent = new ChangeEvent<>();
        final BitmapIndexedNode<E, Void> newRoot = root.remove(getOrCreateMutator(), key, hash(key), 0, changeEvent, TUPLE_LENGTH, this::hash);
        if (changeEvent.isModified) {
            root = newRoot;
            size--;
            modCount++;
            return true;
        }
        return false;
    }

    /**
     * Gets an element from this set. Throws an exception if the set is empty.
     *
     * @return an element
     * @throws java.util.NoSuchElementException if this set is empty
     */
    public E element() {
        return iterator().next();
    }

    /**
     * Removes an element from this set and returns it.
     * Throws an exception if the set is empty.
     *
     * @return an element
     * @throws java.util.NoSuchElementException if this set is empty
     */
    public E remove() {
        Iterator<E> iterator = iterator();
        E e = iterator.next();
        iterator.remove();
        return e;
    }

    /**
     * Removes all specified elements that are in this set.
     *
     * @param c a collection of elements
     * @return {@code true} if this set changed
     */
    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        return removeAll((Iterable<?>) c);
    }

    /**
     * Removes all specified elements that are in this set.
     *
     * @param c an iterable of elements
     * @return {@code true} if this set changed
     */
    public boolean removeAll(@NonNull Iterable<?> c) {
        if (isEmpty()) {
            return false;
        }
        if (c == this) {
            clear();
            return true;
        }
        boolean modified = false;
        for (Object o : c) {
            modified |= remove(o);
        }
        return modified;
    }

    /**
     * {@inheritDoc
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Dumps the internal structure of this set in the Graphviz DOT Language.
     *
     * @return a dump of the internal structure
     */
    public String dump() {
        StringBuilder w = new StringBuilder();
        try {
            ChampTrie.dumpTrieAsGraphviz(w, root, TUPLE_LENGTH, this::hash);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return w.toString();
    }

    /**
     * Returns a persistent copy of this set.
     *
     * @return a persistent trie set
     */
    public PersistentTrieSet<E> toPersistent() {
        mutator = null;
        return size == 0 ? PersistentTrieSet.of() : new PersistentTrieSet<>(root, size);
    }

    static class MutableTrieIterator<E> extends KeyIterator<E, Void> {
        private final @NonNull TrieSet<E> set;
        private int expectedModCount;

        MutableTrieIterator(@NonNull TrieSet<E> set, int tupleLength, ToIntFunction<E> hashFunction) {
            super(set.root, tupleLength, hashFunction);
            this.set = set;
            this.expectedModCount = set.modCount;
        }

        @Override
        public E next() {
            if (expectedModCount != set.modCount) {
                throw new ConcurrentModificationException();
            }
            return super.next();
        }

        @Override
        public void remove() {
            if (expectedModCount != set.modCount) {
                throw new ConcurrentModificationException();
            }
            removeEntry(k -> {
                set.remove(k);
                return set.root;
            });
            expectedModCount = set.modCount;
        }
    }
}