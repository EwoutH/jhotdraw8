/*
 * @(#)IntArrayDeque.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.NonNull;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * IntArrayDeque.
 *
 * @author Werner Randelshofer
 */
public class LongArrayDeque {
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

    public LongArrayDeque() {
        this(8);
    }

    public LongArrayDeque(int capacity) {
        elements = new long[capacity];
    }

    /**
     * Inserts the specified element at the head of this deque.
     *
     * @param e the element to add
     */
    public void addFirst(long e) {
        head = (head - 1) & (elements.length - 1);
        elements[head] = e;
        if (head == tail) {
            doubleCapacity();
        }
    }

    public void clear() {
        if (head <= tail) {
            Arrays.fill(elements, 0, head, tail);
        } else {
            Arrays.fill(elements, 0, 0, tail);
            Arrays.fill(elements, 0, head, elements.length);
        }
        this.head = this.tail = 0;
    }

    /**
     * Inserts the specified element at the head of this deque.
     *
     * @param e the element to add
     */
    public void push(long e) {
        addFirst(e);
    }

    /**
     * Inserts the specified element at the tail of this deque.
     *
     * @param e the element
     */
    public void addLast(long e) {
        elements[tail] = e;
        tail = (tail + 1) & (elements.length - 1);
        if (tail == head) {
            doubleCapacity();
        }
    }

    /**
     * Removes the element at the head of the deque.
     *
     * @throws NoSuchElementException if the queue is empty
     */
    public long removeFirst() {
        if (head == tail) {
            throw new NoSuchElementException();
        }
        long result = elements[head];
        elements[head] = 0;
        head = (head == elements.length - 1) ? 0 : head + 1;
        return result;
    }

    /**
     * Removes the element at the head of the deque.
     *
     * @throws NoSuchElementException if the queue is empty
     */
    public long pop() {
        return removeFirst();
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public long removeLast() {
        if (head == tail) {
            throw new NoSuchElementException();
        }
        tail = (tail == 0) ? elements.length - 1 : tail - 1;
        long result = elements[tail];
        elements[tail] = 0;
        return result;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public long getFirst() {
        if (head == tail) {
            throw new NoSuchElementException();
        }
        long result = elements[head];
        return result;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public long getLast() {
        if (head == tail) {
            throw new NoSuchElementException();
        }
        long result = elements[tail == 0 ? elements.length - 1 : tail - 1];
        return result;
    }

    /**
     * Increases the capacity of this deque.
     */
    private void doubleCapacity() {
        assert head == tail;
        int p = head;
        int n = elements.length;
        int r = n - p; // number of elements to the right of p
        int newCapacity = n << 1;
        if (newCapacity < 0) {
            throw new IllegalStateException("Sorry, deque too big");
        }
        long[] a = new long[newCapacity];
        System.arraycopy(elements, p, a, 0, r);
        System.arraycopy(elements, 0, a, r, p);
        elements = a;
        head = 0;
        tail = n;
    }

    public @NonNull Iterator<Long> iterator() {
        return new DeqIterator();
    }

    private class DeqIterator implements Iterator<Long> {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int cursor = head;

        /**
         * Tail recorded at construction (also in remove), to stop
         * iterator and also to check for comodification.
         */
        private int fence = tail;

        /**
         * Index of element returned by most recent call to next.
         * Reset to -1 if element is deleted by a call to remove.
         */
        private int lastRet = -1;

        public boolean hasNext() {
            return cursor != fence;
        }

        public Long next() {
            if (cursor == fence) {
                throw new NoSuchElementException();
            }
            long result = elements[cursor];
            // This check doesn't catch all possible comodifications,
            // but does catch the ones that corrupt traversal
            if (tail != fence) {
                throw new ConcurrentModificationException();
            }
            lastRet = cursor;
            cursor = (cursor + 1) & (elements.length - 1);
            return result;
        }


    }


    /**
     * Returns the number of elements in this deque.
     *
     * @return the number of elements in this deque
     */
    public int size() {
        return (tail - head) & (elements.length - 1);
    }

    /**
     * Returns true if this deque is empty.
     *
     * @return {@code true} if this deque contains no elements
     */
    public boolean isEmpty() {
        return head == tail;
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
}
