/*
 * @(#)WrappedCollection.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.collection.facade;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.collection.readonly.ReadOnlyCollection;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Wraps {@code Collection} functions into the {@link Collection} interface.
 *
 * @author Werner Randelshofer
 */
public class CollectionFacade<E> extends AbstractCollection<E> implements ReadOnlyCollection<E> {
    protected final @NonNull Supplier<Iterator<E>> iteratorFunction;
    protected final @NonNull IntSupplier sizeFunction;
    protected final @NonNull Predicate<Object> containsFunction;
    protected final @NonNull Runnable clearFunction;
    protected final @NonNull Predicate<Object> removeFunction;

    public CollectionFacade(@NonNull ReadOnlyCollection<E> backingCollection) {
        this(backingCollection::iterator, backingCollection::size,
                backingCollection::contains, null, null);
    }

    public CollectionFacade(@NonNull Collection<E> backingCollection) {
        this(backingCollection::iterator, backingCollection::size,
                backingCollection::contains, backingCollection::clear, backingCollection::remove);
    }

    public CollectionFacade(@NonNull Supplier<Iterator<E>> iteratorFunction,
                            @NonNull IntSupplier sizeFunction,
                            @NonNull Predicate<Object> containsFunction) {
        this(iteratorFunction, sizeFunction, containsFunction, null, null);
    }

    public CollectionFacade(@NonNull Supplier<Iterator<E>> iteratorFunction,
                            @NonNull IntSupplier sizeFunction,
                            @NonNull Predicate<Object> containsFunction,
                            @Nullable Runnable clearFunction,
                            @Nullable Predicate<Object> removeFunction) {
        this.iteratorFunction = iteratorFunction;
        this.sizeFunction = sizeFunction;
        this.containsFunction = containsFunction;
        this.clearFunction = clearFunction == null ? () -> {
            throw new UnsupportedOperationException();
        } : clearFunction;
        this.removeFunction = removeFunction == null ? o -> {
            throw new UnsupportedOperationException();
        } : removeFunction;
    }

    @Override
    public boolean remove(Object o) {
        return removeFunction.test(o);
    }

    @Override
    public void clear() {
        clearFunction.run();
    }

    @Override
    public Stream<E> stream() {
        return super.stream();
    }

    @Override
    public @NonNull Iterator<E> iterator() {
        return iteratorFunction.get();
    }

    @Override
    public int size() {
        return sizeFunction.getAsInt();
    }

    @Override
    public boolean contains(Object o) {
        return containsFunction.test(o);
    }
}
