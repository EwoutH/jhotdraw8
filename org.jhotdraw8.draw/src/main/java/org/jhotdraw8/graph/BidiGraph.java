/*
 * @(#)BidiGraph.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.graph;

import org.jhotdraw8.annotation.NonNull;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * Adds convenience methods to the API defined in {@link BareBidiGraph}.
 *
 * @param <V> the vertex type
 * @param <A> the arrow type
 * @author Werner Randelshofer
 */
public interface BidiGraph<V, A> extends DirectedGraph<V, A>, BareBidiGraph<V, A> {

    /**
     * Returns the direct predecessor arrows of the specified vertex.
     *
     * @param vertex a vertex
     * @return a collection view on the direct predecessor arrows of vertex
     */
    default @NonNull Collection<A> getPrevArrows(@NonNull V vertex) {
        class PrevArrowIterator implements Iterator<A> {

            private int index;
            private final @NonNull V vertex;
            private final int prevCount;

            public PrevArrowIterator(@NonNull V vertex) {
                this.vertex = vertex;
                this.prevCount = getPrevCount(vertex);
            }

            @Override
            public boolean hasNext() {
                return index < prevCount;
            }

            @Override
            public @NonNull A next() {
                return getPrevArrow(vertex, index++);
            }
        }

        return new AbstractCollection<A>() {
            @Override
            public @NonNull Iterator<A> iterator() {
                return new PrevArrowIterator(vertex);
            }

            @Override
            public int size() {
                return getPrevCount(vertex);
            }
        };
    }

    /**
     * Returns the direct predecessor vertices of the specified vertex.
     *
     * @param vertex a vertex
     * @return a collection view on the direct predecessor vertices of vertex
     */
    default @NonNull Collection<V> getPrevVertices(@NonNull V vertex) {
        class PrevVertexIterator implements Iterator<V> {

            private int index;
            private final @NonNull V vertex;
            private final int prevCount;

            public PrevVertexIterator(@NonNull V vertex) {
                this.vertex = vertex;
                this.prevCount = getPrevCount(vertex);
            }

            @Override
            public boolean hasNext() {
                return index < prevCount;
            }

            @Override
            public @NonNull V next() {
                return getPrev(vertex, index++);
            }

        }
        return new AbstractCollection<V>() {
            @Override
            public @NonNull Iterator<V> iterator() {
                return new PrevVertexIterator(vertex);
            }

            @Override
            public int size() {
                return getPrevCount(vertex);
            }
        };
    }
}
