/*
 * @(#)ImmutableSets.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class ImmutableSets {
    public static @NonNull <T> ImmutableSet<T> add(@NonNull Collection<T> collection, T item) {
        switch (collection.size()) {
        case 0:
            return new ImmutableSingletonSet<>(item);
        default:
            Set<T> a = new LinkedHashSet<>(collection);
            a.add(item);
            return new ImmutableHashSet<>(true, a);
        }
    }

    public static @NonNull <T> ImmutableSet<T> add(@NonNull ReadOnlyCollection<T> collection, T item) {
        switch (collection.size()) {
        case 0:
            return new ImmutableSingletonSet<>(item);
        default:
            if (collection.contains(item) && (collection instanceof ImmutableSet)) {
                return (ImmutableSet<T>) collection;
            }
            Set<T> a = new LinkedHashSet<T>(collection.asCollection());
            a.add(item);
            return new ImmutableHashSet<>(true, a);
        }
    }

    @SuppressWarnings("unchecked")
    public static @NonNull <T> ImmutableSet<T> emptySet() {
        return (ImmutableSet<T>) ImmutableHashSet.EMPTY;
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static @NonNull <T> ImmutableSet<T> of(@NonNull T... items) {
        switch (items.length) {
        case 0:
            return emptySet();
        case 1:
            return new ImmutableSingletonSet<>(items[0]);
        default:
            return new ImmutableHashSet<T>(items);
        }
    }

    public static @NonNull <T> ImmutableSet<T> ofCollection(@NonNull Collection<T> collection) {
        switch (collection.size()) {
        case 0:
            return emptySet();
        case 1:
            return new ImmutableSingletonSet<>(collection.iterator().next());
        default:
            return new ImmutableHashSet<T>(collection);
        }
    }

    public static @NonNull <T> ImmutableSet<T> ofCollection(ReadOnlyCollection<T> collection) {
        if (collection instanceof ImmutableSet) {
            return (ImmutableSet<T>) collection;
        }
        switch (collection.size()) {
        case 0:
            return emptySet();
        case 1:
            return new ImmutableSingletonSet<>(collection.iterator().next());
        default:
            return new ImmutableHashSet<T>(collection);
        }
    }

    @SuppressWarnings("unchecked")
    public static @NonNull <T> ImmutableSet<T> ofArray(Object[] a, int offset, int length) {
        switch (length) {
        case 0:
            return emptySet();
        case 1:
            return new ImmutableSingletonSet<>((T) a[offset]);
        default:
            return new ImmutableHashSet<T>(a, offset, length);
        }
    }

    @SuppressWarnings({"unchecked"})
    public static @NonNull <T> ImmutableSet<T> remove(@NonNull Collection<T> collection, T item) {
        switch (collection.size()) {
        case 0:
            return (ImmutableSet<T>) emptySet();
        case 1:
            if (collection.contains(item)) {
                return (ImmutableSet<T>) emptySet();
            } else {
                return ofCollection(collection);
            }
        case 2:
            if (collection.contains(item)) {
                Iterator<T> iter = collection.iterator();
                T one = iter.next();
                T two = iter.next();
                return new ImmutableSingletonSet<>(one.equals(item) ? two : one);

            } else {
                return ofCollection(collection);
            }
        default:
            if (collection.contains(item)) {
                Set<T> a = new LinkedHashSet<>(collection);
                a.remove(item);
                return new ImmutableHashSet<>(true, a);
            } else {
                return ofCollection(collection);
            }
        }
    }

    /**
     * Removes all members from the given collection which are contained in
     * the provided collection {@code c}.
     *
     * @param collection a collection
     * @param c          collection {@code c}.
     * @param <T>        the type of the collections
     * @return a new immutable collection which contains all elements of
     * {@code collection} without the elements that are contained in {@code c}.
     */
    public static @NonNull <T> ImmutableSet<T> removeAll(@NonNull ReadOnlyCollection<T> collection, Collection<T> c) {
        if (collection.isEmpty()) {
            return emptySet();
        }
        if (c.isEmpty()) {
            return ofCollection(collection);
        }

        LinkedHashSet<T> tmp = new LinkedHashSet<>(collection.size());
        for (T elem : collection) {
            if (!c.contains(elem)) {
                tmp.add(elem);
            }
        }
        return ofCollection(tmp);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static @NonNull <T> ImmutableSet<T> remove(@NonNull ReadOnlyCollection<T> collection, T item) {
        switch (collection.size()) {
        case 0:
            return (ImmutableSet<T>) emptySet();
        case 1:
            if (collection.contains(item)) {
                return (ImmutableSet<T>) emptySet();
            } else {
                return ofCollection(collection);
            }
        case 2:
            if (collection.contains(item)) {
                Iterator<T> iter = collection.iterator();
                T one = iter.next();
                T two = iter.next();
                return new ImmutableSingletonSet<>(one.equals(item) ? two : one);
            } else {
                return ofCollection(collection);
            }
        default:
            if (collection.contains(item)) {
                Set<T> a = new LinkedHashSet<>(new CollectionWrapper<>(collection));
                a.remove(item);
                return new ImmutableHashSet<>(true, a);
            } else {
                return ofCollection(collection);
            }
        }
    }
}
