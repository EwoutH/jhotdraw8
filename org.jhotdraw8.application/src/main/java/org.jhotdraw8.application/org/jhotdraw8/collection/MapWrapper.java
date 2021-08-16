/*
 * @(#)MapWrapper.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.NonNull;

import java.util.AbstractMap;
import java.util.Set;

/**
 * Wraps a {@link ReadOnlyMap} in the {@link java.util.Map} API.
 * <p>
 * The underlying ReadOnlyMap is referenced - not copied. This allows to pass a
 * ReadOnlyMap to a client who does not understand the ReadOnlyMap APi.
 *
 * @author Werner Randelshofer
 */
public class MapWrapper<K, V> extends AbstractMap<K, V> {
    private final ReadOnlyMap<K, V> backingMap;

    public MapWrapper(ReadOnlyMap<K, V> backingMap) {
        this.backingMap = backingMap;
    }

    @Override
    public V get(Object key) {
        @SuppressWarnings("unchecked") K unchecked = (K) key;
        return backingMap.get(unchecked);
    }

    @Override
    public boolean containsKey(Object key) {
        @SuppressWarnings("unchecked") K unchecked = (K) key;
        return backingMap.containsKey(unchecked);
    }

    @Override
    public int size() {
        return backingMap.size();
    }

    @Override
    public @NonNull Set<Entry<K, V>> entrySet() {
        return backingMap.entrySet().asSet();
    }
}
