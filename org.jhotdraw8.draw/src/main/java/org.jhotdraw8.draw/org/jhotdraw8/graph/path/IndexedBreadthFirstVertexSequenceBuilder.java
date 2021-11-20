/*
 * @(#)IntAnyPathBuilder.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.graph.path;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.collection.ImmutableList;
import org.jhotdraw8.collection.ImmutableLists;
import org.jhotdraw8.collection.LongArrayDeque;
import org.jhotdraw8.graph.IndexedDirectedGraph;
import org.jhotdraw8.util.function.AddToIntSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/**
 * Builder for creating arbitrary paths from a directed graph.
 * <p>
 * The builder searches for paths using a breadth-first search.<br>
 * Returns the first path that it finds.<br>
 * Returns nothing if there is no path.
 *
 * @author Werner Randelshofer
 */
public class IndexedBreadthFirstVertexSequenceBuilder extends AbstractIndexedVertexSequenceBuilder {


    /**
     * Creates a new instance.
     *
     * @param graph a graph
     */
    public IndexedBreadthFirstVertexSequenceBuilder(@NonNull IndexedDirectedGraph graph) {
        this(graph::getNextVertices);
    }

    /**
     * Creates a new instance.
     *
     * @param nextNodesFunction Accessor function to next nodes in graph.
     */
    public IndexedBreadthFirstVertexSequenceBuilder(@NonNull Function<Integer, Spliterator.OfInt> nextNodesFunction) {
        super(nextNodesFunction);
    }

    /**
     * Enumerates all vertex paths from start to goal up to the specified maximal path length.
     *
     * @param start     the start vertex
     * @param goal      the goal predicate
     * @param maxLength the maximal length of a path
     * @return the enumerated paths
     */
    public @NonNull List<ImmutableList<Integer>> findAllVertexPaths(int start,
                                                                    @NonNull IntPredicate goal,
                                                                    int maxLength) {
        List<MyBackLink> backlinks = new ArrayList<>();
        searchAll(new MyBackLink(start, null, 1), goal,
                getNextNodesFunction(),
                backlinks, maxLength);
        List<ImmutableList<Integer>> vertexPaths = new ArrayList<>(backlinks.size());
        Deque<Integer> path = new ArrayDeque<>();
        for (MyBackLink list : backlinks) {
            path.clear();
            for (MyBackLink backlink = list; backlink != null; backlink = backlink.parent) {
                path.addFirst(backlink.vertex);
            }
            vertexPaths.add(ImmutableLists.copyOf(path));
        }
        return vertexPaths;
    }

    private static class MyIntConsumer implements IntConsumer {
        int value;

        @Override
        public void accept(int value) {
            this.value = value;
        }
    }

    /**
     * Searches breadth-first for a path from root to goal.
     *
     * @param starts    the starting points of the search
     * @param goal      the goal of the search
     * @param visited   a predicate with side effect. The predicate returns true
     *                  if the specified vertex has been visited, and marks the specified vertex
     *                  as visited.
     * @param maxLength the maximal path length
     * @return a back link on success, null on failure
     */
    public @Nullable BackLink search(@NonNull Iterable<Integer> starts,
                                     @NonNull IntPredicate goal,
                                     @NonNull Function<Integer, Spliterator.OfInt> nextNodesFunction,
                                     @NonNull AddToIntSet visited,
                                     int maxLength) {
        Queue<MyBackLink> queue = new ArrayDeque<>(32);
        MyIntConsumer consumer = new MyIntConsumer();
        for (Integer start : starts) {
            MyBackLink rootBackLink = new MyBackLink(start, null, maxLength);
            if (visited.add(start)) {
                queue.add(rootBackLink);
            }
        }

        while (!queue.isEmpty()) {
            MyBackLink node = queue.remove();
            int vertex = node.vertex;
            if (goal.test(vertex)) {
                return node;
            }

            int maxRemaining = node.maxRemaining;
            if (maxRemaining > 0) {
                Spliterator.OfInt spliterator = nextNodesFunction.apply(vertex);
                while (spliterator.tryAdvance(consumer)) {
                    if (visited.add(consumer.value)) {
                        MyBackLink backLink = new MyBackLink(consumer.value, node, maxRemaining - 1);
                        queue.add(backLink);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Searches breadth-first whether a path from root to goal exists.
     *
     * @param root      the starting point of the search
     * @param goal      the goal of the search
     * @param visited   a predicate with side effect. The predicate returns true
     *                  if the specified vertex has been visited, and marks the specified vertex
     *                  as visited.
     * @param maxLength the maximal path length
     * @return true on success, false on failure
     */
    public @Nullable boolean isReachable(@NonNull int root,
                                         @NonNull IntPredicate goal,
                                         @NonNull Function<Integer, Spliterator.OfInt> nextNodesFunction,
                                         @NonNull AddToIntSet visited,
                                         int maxLength) {
        LongArrayDeque queue = new LongArrayDeque(32);
        long rootBackLink = newSearchNode(root, maxLength);

        MyIntConsumer consumer = new MyIntConsumer();
        if (visited.add(root)) {
            queue.addLast(rootBackLink);
        }

        while (!queue.isEmpty()) {
            long node = queue.removeFirst();
            int vertex = searchNodeGetVertex(node);
            if (goal.test(vertex)) {
                return true;
            }

            int maxRemaining = searchNodeGetMaxRemaining(node);
            if (maxRemaining > 0) {
                Spliterator.OfInt spliterator = nextNodesFunction.apply(vertex);
                while (spliterator.tryAdvance(consumer)) {
                    if (visited.add(consumer.value)) {
                        long backLink = newSearchNode(consumer.value, maxRemaining - 1);
                        queue.addLast(backLink);
                    }
                }
            }
        }

        return false;
    }

    /**
     * A SearchNode stores for a given vertex, how long the remaining
     * path to gaol may be until we abort the search.
     *
     * @param vertex       a vertex
     * @param maxRemaining number of remaining path elements until abort
     * @return a SearchNode
     */
    private long newSearchNode(int vertex, int maxRemaining) {
        return (long) vertex << 32 | (long) maxRemaining;
    }

    private int searchNodeGetVertex(long primitiveBackLink) {
        return (int) (primitiveBackLink >> 32);
    }

    private int searchNodeGetMaxRemaining(long primitiveBackLink) {
        return (int) primitiveBackLink;
    }

    private void searchAll(@NonNull MyBackLink start, @NonNull IntPredicate goal,
                           @NonNull Function<Integer, Spliterator.OfInt> nextNodesFunction,
                           @NonNull List<MyBackLink> backlinks, int maxDepth) {
        Deque<MyBackLink> stack = new ArrayDeque<>();
        stack.push(start);
        MyIntConsumer consumer = new MyIntConsumer();
        while (!stack.isEmpty()) {
            MyBackLink current = stack.pop();
            if (goal.test(current.vertex)) {
                backlinks.add(current);
            }
            if (current.maxRemaining < maxDepth) {
                Spliterator.OfInt spliterator = nextNodesFunction.apply(current.vertex);
                while (spliterator.tryAdvance(consumer)) {
                    MyBackLink newPath = new MyBackLink(consumer.value, current, current.maxRemaining + 1);
                    stack.push(newPath);
                }
            }
        }
    }

    private static class MyBackLink extends BackLink {

        final MyBackLink parent;
        final int vertex;
        final int maxRemaining;

        public MyBackLink(int vertex, MyBackLink parent, int depth) {
            this.vertex = vertex;
            this.parent = parent;
            this.maxRemaining = depth;
        }

        @Override
        BackLink getParent() {
            return parent;
        }

        @Override
        int getVertex() {
            return vertex;
        }
    }

}
