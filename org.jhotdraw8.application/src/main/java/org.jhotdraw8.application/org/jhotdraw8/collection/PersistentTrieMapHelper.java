package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Package private class with code for {@link PersistentTrieMap}
 * and {@link TrieMap}.
 */
class PersistentTrieMapHelper {
    static final Node<?, ?> EMPTY_NODE = new BitmapIndexedNode<>(null, (0), (0), new Object[]{});

    /**
     * Don't let anyone instantiate this class.
     */
    private PersistentTrieMapHelper() {
    }

    static final int TUPLE_LENGTH = 2;
    static final int HASH_CODE_LENGTH = 32;
    static final int BIT_PARTITION_SIZE = 5;
    static final int BIT_PARTITION_MASK = 0b11111;

    static abstract class Node<K, V> implements Serializable {
        private final static long serialVersionUID = 0L;
        transient final @Nullable PersistentTrieHelper.Nonce bulkEdit;

        Node(@Nullable PersistentTrieHelper.Nonce bulkEdit) {
            this.bulkEdit = bulkEdit;
        }

        /**
         * Given a bitmap and a bit-position, returns the index
         * in the array.
         * <p>
         * For example, if the bitmap is 0b1101 and
         * bit-position is 0b0100, then the index is 1.
         *
         * @param bitmap a bit-map
         * @param bitpos a bit-position
         * @return the array index
         */
        static int index(final int bitmap, final int bitpos) {
            return Integer.bitCount(bitmap & (bitpos - 1));
        }

        static int bitpos(final int mask) {
            return 1 << mask;
        }

        static boolean isAllowedToEdit(PersistentTrieHelper.Nonce x, PersistentTrieHelper.Nonce y) {
            return x != null && (x == y);
        }

        static int mask(final int keyHash, final int shift) {
            return (keyHash >>> shift) & BIT_PARTITION_MASK;
        }

        static <K, V> Node<K, V> mergeTwoKeyValPairs(PersistentTrieHelper.Nonce bulkEdit, final K key0, final V val0,
                                                     final int keyHash0, final K key1, final V val1, final int keyHash1, final int shift) {
            assert !(key0.equals(key1));

            if (shift >= HASH_CODE_LENGTH) {
                return new HashCollisionNode<>(bulkEdit, keyHash0, new Object[]{key0, val0, key1, val1});
            }

            final int mask0 = mask(keyHash0, shift);
            final int mask1 = mask(keyHash1, shift);

            if (mask0 != mask1) {
                // both nodes fit on same level
                final int dataMap = bitpos(mask0) | bitpos(mask1);

                if (mask0 < mask1) {
                    return new BitmapIndexedNode<>(bulkEdit, (0), dataMap, new Object[]{key0, val0, key1, val1});
                } else {
                    return new BitmapIndexedNode<>(bulkEdit, (0), dataMap, new Object[]{key1, val1, key0, val0});
                }
            } else {
                final Node<K, V> node = mergeTwoKeyValPairs(bulkEdit, key0, val0, keyHash0, key1, val1,
                        keyHash1, shift + BIT_PARTITION_SIZE);
                // values fit on next level

                final int nodeMap = bitpos(mask0);
                return new BitmapIndexedNode<>(bulkEdit, nodeMap, (0), new Object[]{node});
            }
        }

        abstract boolean equivalent(final @NonNull Object other);

        abstract public SearchResult<V> findByKey(final K key, final int keyHash, final int shift);

        abstract K getKey(final int index);

        abstract Map.Entry<K, V> getKeyValueEntry(final int index);

        abstract Node<K, V> getNode(final int index);

        abstract V getValue(final int index);

        abstract boolean hasNodes();

        abstract boolean hasPayload();

        abstract int nodeArity();

        /**
         * Returns the node index for the given keyHash and shift, or -1.
         *
         * @param keyHash the key hash
         * @param shift   the shift
         * @return the node index or -1
         */
        abstract int nodeIndex(final int keyHash, final int shift);

        abstract int payloadArity();

        /**
         * Returns the payload index for the given keyHash and shift, or -1.
         *
         * @param key
         * @param keyHash the key hash
         * @param shift   the shift
         * @return the payload index or -1
         */
        abstract int payloadIndex(@Nullable K key, final int keyHash, final int shift);

        abstract public Node<K, V> removed(final @Nullable PersistentTrieHelper.Nonce bulkEdit, final K key,
                                           final int keyHash, final int shift, final ChangeEvent<V> details);

        abstract PersistentTrieHelper.SizeClass sizePredicate();

        abstract public Node<K, V> updated(final PersistentTrieHelper.Nonce bulkEdit, final K key, final V val,
                                           final int keyHash, final int shift, final ChangeEvent<V> details);

    }

    private static final class BitmapIndexedNode<K, V> extends Node<K, V> {
        private final static long serialVersionUID = 0L;
        private final Object[] nodes;
        private final int nodeMap;
        private final int dataMap;

        private BitmapIndexedNode(final PersistentTrieHelper.Nonce bulkEdit, final int nodeMap,
                                  final int dataMap, final Object[] nodes) {
            super(bulkEdit);
            this.nodeMap = nodeMap;
            this.dataMap = dataMap;
            this.nodes = nodes;
        }

        Node<K, V> copyAndInsertValue(final PersistentTrieHelper.Nonce bulkEdit, final int bitpos,
                                      final K key, final V val) {
            final int idx = TUPLE_LENGTH * dataIndex(bitpos);

            // copy 'src' and insert 2 element(s) at position 'idx'
            final Object[] dst = PersistentTrieHelper.copyComponentAdd(this.nodes, idx, 2);
            dst[idx] = key;
            dst[idx + 1] = val;
            return new BitmapIndexedNode<>(bulkEdit, nodeMap(), dataMap() | bitpos, dst);
        }

        Node<K, V> copyAndMigrateFromInlineToNode(final PersistentTrieHelper.Nonce bulkEdit,
                                                  final int bitpos, final Node<K, V> node) {

            final int idxOld = TUPLE_LENGTH * dataIndex(bitpos);
            final int idxNew = this.nodes.length - TUPLE_LENGTH - nodeIndex(bitpos);
            assert idxOld <= idxNew;

            // copy 'src' and remove 2 element(s) at position 'idxOld' and
            // insert 1 element(s) at position 'idxNew'
            final Object[] src = this.nodes;
            final Object[] dst = new Object[src.length - 2 + 1];
            System.arraycopy(src, 0, dst, 0, idxOld);
            System.arraycopy(src, idxOld + 2, dst, idxOld, idxNew - idxOld);
            System.arraycopy(src, idxNew + 2, dst, idxNew + 1, src.length - idxNew - 2);
            dst[idxNew] = node;

            return new BitmapIndexedNode<>(bulkEdit, nodeMap() | bitpos, dataMap() ^ bitpos, dst);
        }

        Node<K, V> copyAndMigrateFromNodeToInline(final PersistentTrieHelper.Nonce bulkEdit,
                                                  final int bitpos, final Node<K, V> node) {

            final int idxOld = this.nodes.length - 1 - nodeIndex(bitpos);
            final int idxNew = TUPLE_LENGTH * dataIndex(bitpos);

            // copy 'src' and remove 1 element(s) at position 'idxOld' and
            // insert 2 element(s) at position 'idxNew'
            final Object[] src = this.nodes;
            final Object[] dst = new Object[src.length - 1 + 2];
            assert idxOld >= idxNew;
            System.arraycopy(src, 0, dst, 0, idxNew);
            System.arraycopy(src, idxNew, dst, idxNew + 2, idxOld - idxNew);
            System.arraycopy(src, idxOld + 1, dst, idxOld + 2, src.length - idxOld - 1);
            dst[idxNew] = node.getKey(0);
            dst[idxNew + 1] = node.getValue(0);

            return new BitmapIndexedNode<>(bulkEdit, nodeMap() ^ bitpos, dataMap() | bitpos, dst);
        }

        Node<K, V> copyAndRemoveValue(final PersistentTrieHelper.Nonce bulkEdit,
                                      final int bitpos) {
            final int idx = TUPLE_LENGTH * dataIndex(bitpos);

            // copy 'src' and remove 2 element(s) at position 'idx'
            final Object[] dst = PersistentTrieHelper.copyComponentRemove(this.nodes, idx, 2);
            return new BitmapIndexedNode<>(bulkEdit, nodeMap(), dataMap() ^ bitpos, dst);
        }

        Node<K, V> copyAndSetNode(final PersistentTrieHelper.Nonce bulkEdit, final int bitpos,
                                  final Node<K, V> node) {

            final int idx = this.nodes.length - 1 - nodeIndex(bitpos);

            if (isAllowedToEdit(this.bulkEdit, bulkEdit)) {
                // no copying if already editable
                this.nodes[idx] = node;
                return this;
            } else {
                // copy 'src' and set 1 element(s) at position 'idx'
                final Object[] dst = PersistentTrieHelper.copySet(this.nodes, idx, node);
                return new BitmapIndexedNode<>(bulkEdit, nodeMap(), dataMap(), dst);
            }
        }

        Node<K, V> copyAndSetValue(final PersistentTrieHelper.Nonce bulkEdit, final int bitpos,
                                   final V val) {
            final int idx = TUPLE_LENGTH * dataIndex(bitpos) + 1;

            if (isAllowedToEdit(this.bulkEdit, bulkEdit)) {
                // no copying if already editable
                this.nodes[idx] = val;
                return this;
            } else {
                // copy 'src' and set 1 element(s) at position 'idx'
                final Object[] dst = PersistentTrieHelper.copySet(this.nodes, idx, val);
                return new BitmapIndexedNode<>(bulkEdit, nodeMap(), dataMap(), dst);
            }
        }

        int dataIndex(final int bitpos) {
            return Integer.bitCount(dataMap() & (bitpos - 1));
        }

        public int dataMap() {
            return dataMap;
        }

        @Override
        public boolean equivalent(final @NonNull Object other) {
            if (this == other) {
                return true;
            }
            BitmapIndexedNode<?, ?> that = (BitmapIndexedNode<?, ?>) other;

            // nodes array: we compare local payload from 0 to splitAt (excluded)
            // and then we compare the nested nodes from splitAt to length (excluded)
            int splitAt = 2 * payloadArity();
            return nodeMap() == that.nodeMap()
                    && dataMap() == that.dataMap()
                    && ArrayHelper.equals(nodes, 0, splitAt, that.nodes, 0, splitAt)
                    && ArrayHelper.equals(nodes, splitAt, nodes.length, that.nodes, splitAt, that.nodes.length,
                    (a, b) -> ((Node<?, ?>) a).equivalent(b));
        }

        @Override
        public SearchResult<V> findByKey(final K key, final int keyHash, final int shift) {
            final int mask = mask(keyHash, shift);
            final int bitpos = bitpos(mask);

            if ((dataMap() & bitpos) != 0) { // inplace value
                final int index = dataIndex(bitpos);
                if (Objects.equals(getKey(index), key)) {
                    final V result = getValue(index);

                    return new SearchResult<>(result, true);
                }

                return new SearchResult<>(null, false);
            }

            if ((nodeMap() & bitpos) != 0) { // node (not value)
                final Node<K, V> subNode = nodeAt(bitpos);

                return subNode.findByKey(key, keyHash, shift + BIT_PARTITION_SIZE);
            }

            return new SearchResult<>(null, false);
        }

        @Override
        @SuppressWarnings("unchecked")
        K getKey(final int index) {
            return (K) nodes[TUPLE_LENGTH * index];
        }

        @Override
        @SuppressWarnings("unchecked")
        Map.Entry<K, V> getKeyValueEntry(final int index) {
            return new AbstractMap.SimpleImmutableEntry<>((K) nodes[TUPLE_LENGTH * index], (V) nodes[TUPLE_LENGTH * index + 1]);
        }

        @Override
        @SuppressWarnings("unchecked")
        Node<K, V> getNode(final int index) {
            return (Node<K, V>) nodes[nodes.length - 1 - index];
        }

        @Override
        @SuppressWarnings("unchecked")
        V getValue(final int index) {
            return (V) nodes[TUPLE_LENGTH * index + 1];
        }

        @Override
        boolean hasNodes() {
            return nodeMap() != 0;
        }

        @Override
        boolean hasPayload() {
            return dataMap() != 0;
        }

        @Override
        int nodeArity() {
            return Integer.bitCount(nodeMap());
        }

        Node<K, V> nodeAt(final int bitpos) {
            return getNode(nodeIndex(bitpos));
        }

        int nodeIndex(final int bitpos) {
            return Integer.bitCount(nodeMap() & (bitpos - 1));
        }

        @Override
        int nodeIndex(int keyHash, int shift) {
            final int mask = mask(keyHash, shift);
            final int bitpos = bitpos(mask);
            return (this.nodeMap & bitpos) != 0
                    ? index(this.nodeMap, bitpos)
                    : -1;
        }

        public int nodeMap() {
            return nodeMap;
        }

        @Override
        int payloadArity() {
            return Integer.bitCount(dataMap());
        }

        @Override
        int payloadIndex(K key, final int keyHash, final int shift) {
            final int mask = mask(keyHash, shift);
            final int bitpos = bitpos(mask);
            return (this.dataMap & bitpos) != 0
                    ? index(this.dataMap, bitpos)
                    : -1;
        }

        @Override
        public Node<K, V> removed(final @Nullable PersistentTrieHelper.Nonce bulkEdit, final K key,
                                  final int keyHash, final int shift, final ChangeEvent<V> details) {
            final int mask = mask(keyHash, shift);
            final int bitpos = bitpos(mask);

            if ((dataMap() & bitpos) != 0) { // inplace value
                final int dataIndex = dataIndex(bitpos);

                if (Objects.equals(getKey(dataIndex), key)) {
                    final V currentVal = getValue(dataIndex);
                    details.updated(currentVal);

                    if (this.payloadArity() == 2 && this.nodeArity() == 0) {
                        // Create new node with remaining pair. The new node will a) either become the new root
                        // returned, or b) unwrapped and inlined during returning.
                        final int newDataMap =
                                (shift == 0) ? (dataMap() ^ bitpos) : bitpos(mask(keyHash, 0));

                        if (dataIndex == 0) {
                            return new BitmapIndexedNode<>(bulkEdit, (0), newDataMap, new Object[]{getKey(1), getValue(1)});
                        } else {
                            return new BitmapIndexedNode<>(bulkEdit, (0), newDataMap, new Object[]{getKey(0), getValue(0)});
                        }
                    } else {
                        return copyAndRemoveValue(bulkEdit, bitpos);
                    }
                } else {
                    return this;
                }
            } else if ((nodeMap() & bitpos) != 0) { // node (not value)
                final Node<K, V> subNode = nodeAt(bitpos);
                final Node<K, V> subNodeNew =
                        subNode.removed(bulkEdit, key, keyHash, shift + BIT_PARTITION_SIZE, details);

                if (!details.isModified()) {
                    return this;
                }

                switch (subNodeNew.sizePredicate()) {
                    case SIZE_EMPTY: {
                        throw new IllegalStateException("Sub-node must have at least one element.");
                    }
                    case SIZE_ONE: {
                        if (this.payloadArity() == 0 && this.nodeArity() == 1) {
                            // escalate (singleton or empty) result
                            return subNodeNew;
                        } else {
                            // inline value (move to front)
                            return copyAndMigrateFromNodeToInline(bulkEdit, bitpos, subNodeNew);
                        }
                    }
                    default: {
                        // modify current node (set replacement node)
                        return copyAndSetNode(bulkEdit, bitpos, subNodeNew);
                    }
                }
            }

            return this;
        }

        @Override
        public PersistentTrieHelper.SizeClass sizePredicate() {
            if (this.nodeArity() == 0) {
                switch (this.payloadArity()) {
                    case 0:
                        return PersistentTrieHelper.SizeClass.SIZE_EMPTY;
                    case 1:
                        return PersistentTrieHelper.SizeClass.SIZE_ONE;
                    default:
                        return PersistentTrieHelper.SizeClass.SIZE_MORE_THAN_ONE;
                }
            } else {
                return PersistentTrieHelper.SizeClass.SIZE_MORE_THAN_ONE;
            }
        }

        @Override
        public Node<K, V> updated(final PersistentTrieHelper.Nonce bulkEdit, final K key, final V val,
                                  final int keyHash, final int shift, final ChangeEvent<V> details) {
            final int mask = mask(keyHash, shift);
            final int bitpos = bitpos(mask);

            if ((dataMap() & bitpos) != 0) { // inplace value
                final int dataIndex = dataIndex(bitpos);
                final K currentKey = getKey(dataIndex);

                final V currentVal = getValue(dataIndex);
                if (Objects.equals(currentKey, key)) {
                    if (Objects.equals(currentVal, val)) {
                        details.found(currentVal);
                        return this;
                    }
                    // update mapping
                    details.updated(currentVal);
                    return copyAndSetValue(bulkEdit, bitpos, val);
                } else {
                    final Node<K, V> subNodeNew =
                            mergeTwoKeyValPairs(bulkEdit, currentKey, currentVal, currentKey.hashCode(),
                                    key, val, keyHash, shift + BIT_PARTITION_SIZE);

                    details.modified();
                    return copyAndMigrateFromInlineToNode(bulkEdit, bitpos, subNodeNew);
                }
            } else if ((nodeMap() & bitpos) != 0) { // node (not value)
                final Node<K, V> subNode = nodeAt(bitpos);
                final Node<K, V> subNodeNew =
                        subNode.updated(bulkEdit, key, val, keyHash, shift + BIT_PARTITION_SIZE, details);

                if (details.isModified()) {
                    return copyAndSetNode(bulkEdit, bitpos, subNodeNew);
                } else {
                    return this;
                }
            } else {
                // no value
                details.modified();
                return copyAndInsertValue(bulkEdit, bitpos, key, val);
            }
        }

    }

    private static final class HashCollisionNode<K, V> extends Node<K, V> {
        private final static long serialVersionUID = 0L;
        private final int hash;
        private @NonNull Object[] entries;

        HashCollisionNode(PersistentTrieHelper.Nonce bulkEdit, final int hash, final Object[] entries) {
            super(bulkEdit);
            this.entries = entries;
            this.hash = hash;

            assert payloadArity() >= 2;
        }

        @Override
        int nodeIndex(int keyHash, int shift) {
            return -1;
        }

        @Override
        public boolean equivalent(@NonNull Object other) {
            if (this == other) {
                return true;
            }
            HashCollisionNode<?, ?> that = (HashCollisionNode<?, ?>) other;
            if (hash != that.hash
                    || payloadArity() != that.payloadArity()) {
                return false;
            }

            // Linear scan for each key, because of arbitrary element order.
            outerLoop:
            for (int i = 0, n = that.payloadArity(); i < n; i++) {
                final Object otherKey = that.getKey(i);
                final Object otherVal = that.getValue(i);

                for (int j = 0, m = payloadArity(); j < m; j++) {
                    final K key = getKey(j);
                    final V val = getValue(j);

                    if (Objects.equals(key, otherKey) && Objects.equals(val, otherVal)) {
                        continue outerLoop;
                    }
                }
                return false;
            }

            return true;
        }

        @Override
        public SearchResult<V> findByKey(final K key, final int keyHash, final int shift) {
            for (int i = 0, n = payloadArity(); i < n; i++) {
                final K _key = getKey(i);
                if (Objects.equals(key, _key)) {
                    final V val = getValue(i);
                    return new SearchResult<>(val, true);
                }
            }
            return new SearchResult<>(null, false);
        }

        @Override
        @SuppressWarnings("unchecked")
        K getKey(final int index) {
            return (K) entries[index * 2];
        }

        @SuppressWarnings("unchecked")
        @Override
        Map.Entry<K, V> getKeyValueEntry(final int index) {
            return new AbstractMap.SimpleImmutableEntry<>((K) entries[index * 2], (V) entries[index * 2 + 1]);
        }

        @Override
        public Node<K, V> getNode(int index) {
            throw new IllegalStateException("Is leaf node.");
        }

        @SuppressWarnings("unchecked")
        @Override
        V getValue(final int index) {
            return (V) entries[index * 2 + 1];
        }

        @Override
        boolean hasNodes() {
            return false;
        }

        @Override
        boolean hasPayload() {
            return true;
        }

        @Override
        int nodeArity() {
            return 0;
        }

        @Override
        int payloadArity() {
            return entries.length >> 1;
        }

        @Override
        int payloadIndex(K key, final int keyHash, final int shift) {
            if (this.hash != keyHash) {
                return -1;
            }
            for (int i = 0, n = payloadArity(); i < n; i++) {
                K k = getKey(i);
                if (Objects.equals(k, key)) {
                    return i;
                }
            }
            return -1;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Node<K, V> removed(final @Nullable PersistentTrieHelper.Nonce bulkEdit, final K key,
                                  final int keyHash, final int shift, final ChangeEvent<V> details) {
            for (int idx = 0, n = payloadArity(); idx < n; idx++) {
                if (Objects.equals(getKey(idx), key)) {
                    final V currentVal = getValue(idx);
                    details.updated(currentVal);

                    if (payloadArity() == 1) {
                        return emptyNode();
                    } else if (payloadArity() == 2) {
                        // Create root node with singleton element.
                        // This node will be a) either be the new root
                        // returned, or b) unwrapped and inlined.
                        final K theOtherKey = (K) ((idx == 0) ? entries[2] : entries[0]);
                        final V theOtherVal = (V) ((idx == 0) ? entries[3] : entries[1]);
                        return PersistentTrieMapHelper.<K, V>emptyNode().updated(bulkEdit, theOtherKey, theOtherVal,
                                keyHash, 0, details);
                    } else {

                        // copy keys and vals and remove 1 element at position idx
                        final Object[] entriesNew = PersistentTrieHelper.copyComponentRemove(this.entries, idx * 2, 2);

                        if (this.bulkEdit == bulkEdit) {
                            this.entries = entriesNew;
                            return this;
                        }
                        return new HashCollisionNode<>(bulkEdit, keyHash, entriesNew);
                    }
                }
            }
            return this;
        }

        @Override
        public PersistentTrieHelper.SizeClass sizePredicate() {
            return PersistentTrieHelper.SizeClass.SIZE_MORE_THAN_ONE;
        }

        @Override
        public Node<K, V> updated(final PersistentTrieHelper.Nonce bulkEdit, final K key, final V val,
                                  final int keyHash, final int shift, final ChangeEvent<V> details) {
            assert this.hash == keyHash;

            for (int idx = 0, n = payloadArity(); idx < n; idx++) {
                if (Objects.equals(getKey(idx), key)) {
                    final V currentVal = getValue(idx);
                    if (Objects.equals(currentVal, val)) {
                        details.found(currentVal);
                        return this;
                    } else {
                        final Object[] dst = PersistentTrieHelper.copySet(this.entries, idx * 2 + 1, val);
                        final Node<K, V> thisNew = new HashCollisionNode<>(bulkEdit, this.hash, dst);
                        details.updated(currentVal);
                        return thisNew;
                    }
                }
            }

            // copy keys and vals and add 1 element at the end
            final Object[] entriesNew = PersistentTrieHelper.copyComponentAdd(this.entries, this.entries.length, 2);
            entriesNew[this.entries.length] = key;
            entriesNew[this.entries.length + 1] = val;
            details.modified();
            if (isAllowedToEdit(this.bulkEdit, bulkEdit)) {
                this.entries = entriesNew;
                return this;
            } else {
                return new HashCollisionNode<>(bulkEdit, keyHash, entriesNew);
            }
        }

    }

    /**
     * Iterator skeleton that uses a fixed stack in depth.
     */
    static abstract class AbstractMapIterator<K, V> {

        private static final int MAX_DEPTH = 7;
        private final int[] nodeCursorsAndLengths = new int[MAX_DEPTH * 2];
        protected int nextValueCursor;
        protected int nextValueLength;
        protected Node<K, V> nextValueNode;
        private int nextStackLevel = -1;
        protected Map.Entry<K, V> current;

        @SuppressWarnings({"unchecked"})
        Node<K, V>[] nodes = new Node[MAX_DEPTH];

        AbstractMapIterator(Node<K, V> rootNode) {
            if (rootNode.hasNodes()) {
                nextStackLevel = 0;

                nodes[0] = rootNode;
                nodeCursorsAndLengths[0] = 0;
                nodeCursorsAndLengths[1] = rootNode.nodeArity();
            }

            if (rootNode.hasPayload()) {
                nextValueNode = rootNode;
                nextValueCursor = 0;
                nextValueLength = rootNode.payloadArity();
            }
        }

        public boolean hasNext() {
            if (nextValueCursor < nextValueLength) {
                return true;
            } else {
                return searchNextValueNode();
            }
        }

        protected Map.Entry<K, V> nextEntry() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            } else {
                return current = nextValueNode.getKeyValueEntry(nextValueCursor++);
            }
        }

        /**
         * Moves the iterator so that it stands before the specified
         * element.
         *
         * @param k        an element
         * @param rootNode the root node of the set
         */
        protected void moveTo(final @Nullable K k, final @NonNull Node<K, V> rootNode) {
            int keyHash = Objects.hashCode(k);
            int shift = 0;
            Node<K, V> node = rootNode;

            nextStackLevel = -1;
            nextValueNode = null;
            nextValueCursor = 0;
            nextValueLength = 0;
            Arrays.fill(nodes, null);
            Arrays.fill(nodeCursorsAndLengths, 0);
            current = null;

            for (int depth = 0; depth < MAX_DEPTH; depth++) {
                nodes[depth] = node;

                int nodeIndex = node.nodeIndex(keyHash, shift);
                if (nodeIndex != -1) {
                    final int nextCursorIndex = depth * 2;
                    final int nextLengthIndex = nextCursorIndex + 1;
                    nodeCursorsAndLengths[nextCursorIndex] = 0;
                    nodeCursorsAndLengths[nextLengthIndex] = node.nodeArity();
                    node = node.getNode(nodeIndex);
                } else {
                    int payloadIndex = node.payloadIndex(k, keyHash, shift);
                    if (payloadIndex != -1) {
                        nextValueNode = node;
                        nextValueCursor = payloadIndex;
                        nextValueLength = node.payloadArity();
                        nextStackLevel = depth;
                    }
                    break;
                }

                shift += BIT_PARTITION_SIZE;
            }
        }


        /*
         * search for next node that contains values
         */
        private boolean searchNextValueNode() {
            while (nextStackLevel >= 0) {
                final int currentCursorIndex = nextStackLevel * 2;
                final int currentLengthIndex = currentCursorIndex + 1;

                final int nodeCursor = nodeCursorsAndLengths[currentCursorIndex];
                final int nodeLength = nodeCursorsAndLengths[currentLengthIndex];

                if (nodeCursor < nodeLength) {
                    final Node<K, V> nextNode = nodes[nextStackLevel].getNode(nodeCursor);
                    nodeCursorsAndLengths[currentCursorIndex]++;

                    if (nextNode.hasNodes()) {
                        /*
                         * put node on next stack level for depth-first traversal
                         */
                        final int nextStackLevel = ++this.nextStackLevel;
                        final int nextCursorIndex = nextStackLevel * 2;
                        final int nextLengthIndex = nextCursorIndex + 1;

                        nodes[nextStackLevel] = nextNode;
                        nodeCursorsAndLengths[nextCursorIndex] = 0;
                        nodeCursorsAndLengths[nextLengthIndex] = nextNode.nodeArity();
                    }

                    if (nextNode.hasPayload()) {
                        /*
                         * found next node that contains values
                         */
                        nextValueNode = nextNode;
                        nextValueCursor = 0;
                        nextValueLength = nextNode.payloadArity();
                        return true;
                    }
                } else {
                    nextStackLevel--;
                }
            }

            return false;
        }
    }

    protected static class MapKeyIterator<K, V> extends AbstractMapIterator<K, V>
            implements Iterator<K> {

        MapKeyIterator(Node<K, V> rootNode) {
            super(rootNode);
        }

        @Override
        public K next() {
            return nextEntry().getKey();
        }

    }

    protected static class MapEntryIterator<K, V> extends AbstractMapIterator<K, V>
            implements Iterator<Map.Entry<K, V>> {

        MapEntryIterator(Node<K, V> rootNode) {
            super(rootNode);
        }

        @Override
        public Map.Entry<K, V> next() {
            return nextEntry();
        }

    }

    static class SearchResult<V> {
        private final @Nullable V value;
        private final boolean keyExists;

        public SearchResult(@Nullable V value, boolean keyExists) {
            this.value = value;
            this.keyExists = keyExists;
        }

        public @Nullable V get() {
            if (!keyExists) {
                throw new NoSuchElementException();
            }
            return value;
        }

        public boolean keyExists() {
            return keyExists;
        }

        public @Nullable V orElse(@Nullable V elseValue) {
            return keyExists ? value : elseValue;
        }
    }

    static class ChangeEvent<V> {

        private V oldValue;
        boolean isModified;
        private boolean isReplaced;

        ChangeEvent() {
        }

        public V getOldValue() {
            return oldValue;
        }

        public boolean hasReplacedValue() {
            return isReplaced;
        }

        public boolean isModified() {
            return isModified;
        }

        // update: inserted/removed single element, element count changed
        public void modified() {
            this.isModified = true;
        }

        public void updated(V oldValue) {
            this.oldValue = oldValue;
            this.isModified = true;
            this.isReplaced = true;
        }

        public void found(V oldValue) {
            this.oldValue = oldValue;
        }
    }

    @SuppressWarnings("unchecked")
    static <K, V> PersistentTrieMapHelper.Node<K, V> emptyNode() {
        return (PersistentTrieMapHelper.Node<K, V>) PersistentTrieMapHelper.EMPTY_NODE;
    }


}