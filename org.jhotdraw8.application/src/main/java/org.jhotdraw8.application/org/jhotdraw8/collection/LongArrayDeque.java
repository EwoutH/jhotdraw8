/*
 * @(#)LongArrayDeque.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.util.Preconditions;

import java.util.*;

/**
 * LongArrayDeque.
 *
 * @author Werner Randelshofer
 */
public class LongArrayDeque extends AbstractCollection<Long> implements LongDeque {
    /**
     * The length of this array is always a power of 2.
     */
    private long[] elements;

    /**
     * Index of the element at the head of the deque.
     */
    private int head;

    /**
     * Index at which the next element would be added to the tail of the deque.
     */
    private int tail;

    /**
     * Creates a new instance with an initial capacity for 8 elements.
     */
    public LongArrayDeque() {
        this(8);
    }

    /**
     * Creates a new instance with the specified initial capacity rounded up
     * to the next strictly positive power of two.
     *
     * @param capacity initial capacity
     */
    public LongArrayDeque(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity=" + capacity);
        }
        int size = Integer.highestOneBit(capacity + capacity - 1);
        elements = new long[Math.max(size, 0)];
    }

    /**
     * Inserts the specified element at the head of this deque.
     *
     * @param e the element to add
     */
    public void addFirstAsLong(long e) {
        //Note: elements.length is a power of two.
        head = (head - 1) & (elements.length - 1);
        elements[head] = e;
        if (head == tail) {
            grow(size() + 1);
        }
    }

    public void addLastAllAsLong(long[] array) {
        addLastAllAsLong(array, 0, array.length);
    }

    public void addLastAllAsLong(long[] array, int offset, int length) {
        grow(size() + length);

        int firstPart = elements.length - tail;
        if (tail >= head && firstPart >= length
                || head - tail > length) {
            System.arraycopy(array, offset, elements, tail, length);
            tail = (tail + length) & (elements.length - 1);
            return;
        }

        System.arraycopy(array, offset, elements, tail, firstPart);
        int secondPart = length - firstPart;
        System.arraycopy(array, offset + firstPart, elements, 0, secondPart);
        tail = secondPart;
    }

    /**
     * Inserts the specified element at the tail of this deque.
     *
     * @param e the element
     */
    public void addLastAsLong(long e) {
        //Note: elements.length is a power of two.
        elements[tail] = e;
        tail = (tail + 1) & (elements.length - 1);
        if (tail == head) {
            grow(size() + 1);
        }
    }

    public void clear() {
        if (head < tail) {
            Arrays.fill(elements, head, tail + 1, 0);
        } else {
            Arrays.fill(elements, 0, tail + 1, 0);
            Arrays.fill(elements, head, elements.length, 0);
        }
        this.head = this.tail = 0;
    }

    @Override
    public Iterator<Long> descendingIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long element() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return getFirstAsLong();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LongArrayDeque)) {
            return false;
        }
        LongArrayDeque that = (LongArrayDeque) o;
        if (this.size() != that.size()) {
            return false;
        }
        int thisMask = elements.length - 1;
        int thatMask = that.elements.length - 1;
        for (int i = this.head, j = that.head; i != this.tail; i = (i + 1) & thisMask, j = (j + 1) & thatMask) {
            if (this.elements[i] != that.elements[j]) {
                return false;
            }
        }
        return true;
    }

    public int firstIndexOfAsLong(long o) {
        if (tail < head) {
            for (int i = head; i < elements.length; i++) {
                if (o == (elements[i])) {
                    return i - head;
                }
            }
            for (int i = 0; i < tail; i++) {
                if (o == (elements[i])) {
                    return i + elements.length - head;
                }
            }
        } else {
            for (int i = head; i < tail; i++) {
                if (o == (elements[i])) {
                    return i - head;
                }
            }
        }
        return -1;
    }

    /**
     * @throws NoSuchElementException if the queue is empty
     */
    public long getFirstAsLong() {
        if (head == tail) {
            throw new NoSuchElementException();
        }
        return elements[head];
    }

    /**
     * @throws NoSuchElementException if the queue is empty
     */
    public long getLastAsLong() {
        if (head == tail) {
            throw new NoSuchElementException();
        }
        return elements[tail == 0 ? elements.length - 1 : tail - 1];
    }

    /**
     * Increases the capacity of this deque.
     */
    private void grow(int capacity) {
        if (elements.length >= capacity) {
            return;
        }
        //assert head == tail;
        int size = size();
        int p = head;
        int n = elements.length;
        int r = n - p; // number of elements to the right of p
        int newCapacity = Math.max(1, Integer.highestOneBit(capacity + capacity - 1));
        if (newCapacity < capacity) {
            throw new IllegalStateException("Sorry, deque too big");
        }
        long[] a = new long[newCapacity];
        System.arraycopy(elements, p, a, 0, r);
        System.arraycopy(elements, 0, a, r, p);
        elements = a;
        head = 0;
        tail = size;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        int mask = elements.length - 1;
        for (int i = head; i != tail; i = (i + 1) & mask) {
            hash = hash * 31 + (int) elements[i];
        }
        return hash;
    }

    /**
     * Returns true if this deque is empty.
     *
     * @return {@code true} if this deque contains no elements
     */
    public boolean isEmpty() {
        return head == tail;
    }

    public @NonNull Iterator<Long> iterator() {
        return new DeqIterator();
    }

    public int lastIndexOfAsLong(long o) {
        if (tail < head) {
            for (int i = elements.length - 1; i >= head; i--) {
                if (o == (elements[i])) {
                    return i - head;
                }
            }
            for (int i = tail - 1; i >= 0; i--) {
                if (o == (elements[i])) {
                    return i + elements.length - head;
                }
            }
        } else {
            for (int i = tail - 1; i >= head; i--) {
                if (o == (elements[i])) {
                    return i - head;
                }
            }
        }
        return -1;
    }

    /**
     * Removes an element at the given array index.
     */
    public void removeAt(int i) {
        int size = size();
        Preconditions.checkIndex(i, size);
        if (tail < head) {
            if (head + i < elements.length) {
                if (i > 0) {
                    System.arraycopy(elements, head, elements, head + 1, i - 1);
                }
                elements[head] = 0;
                head = head == elements.length ? 0 : head + 1;
            } else {
                if (i < size - 1) {
                    System.arraycopy(elements, i - elements.length + head + 1, elements, i - elements.length + head, size - i);
                }
                elements[tail] = 0;
                tail = tail == 0 ? elements.length : tail - 1;
            }
        } else {
            if (i < size - 1) {
                System.arraycopy(elements, head + i + 1, elements, head + i, size - i);
            }
            elements[head + i] = 0;
            tail--;
        }
    }

    /**
     * Removes the element at the head of the deque.
     *
     * @throws NoSuchElementException if the queue is empty
     */
    public long removeFirstAsLong() {
        if (head == tail) {
            throw new NoSuchElementException();
        }
        long result = elements[head];
        elements[head] = 0;
        head = (head == elements.length - 1) ? 0 : head + 1;
        return result;
    }

    public boolean removeFirstOccurrenceAsLong(long o) {
        int index = firstIndexOfAsLong(o);
        if (index != -1) {
            removeAt(index);
            return true;
        }
        return false;
    }

    public long removeLastAsLong() {
        if (head == tail) {
            throw new NoSuchElementException();
        }
        tail = (tail == 0) ? elements.length - 1 : tail - 1;
        long result = elements[tail];
        elements[tail] = 0;
        return result;
    }

    @Override
    public boolean removeLastOccurrenceAsLong(long o) {
        int index = lastIndexOfAsLong((int) o);
        if (index != -1) {
            removeAt(index);
            return true;
        }
        return false;
    }

    public int size() {
        return (tail - head) & (elements.length - 1);
    }

    public @NonNull String toString() {
        Iterator<Long> it = iterator();
        if (!it.hasNext()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (; ; ) {
            Long e = it.next();
            sb.append(e);
            if (!it.hasNext()) {
                return sb.append(']').toString();
            }
            sb.append(',').append(' ');
        }
    }

    private class DeqIterator implements Iterator<Long> {
        /**
         * Tail recorded at construction, to stop
         * iterator and also to check for co-modification.
         */
        private final int fence = tail;
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int cursor = head;

        public boolean hasNext() {
            return cursor != fence;
        }

        public Long next() {
            if (cursor == fence) {
                throw new NoSuchElementException();
            }
            long result = elements[cursor];
            // This check doesn't catch all possible co-modifications,
            // but does catch the ones that corrupt traversal
            if (tail != fence) {
                throw new ConcurrentModificationException();
            }
            cursor = (cursor + 1) & (elements.length - 1);
            return result;
        }
    }
}
