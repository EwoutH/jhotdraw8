/*
 * @(#)AnyShortestPathBuilder.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.graph;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.util.ToDoubleTriFunction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;

/**
 * This path builder can be used to find any shortest path between
 * to vertices in a directed graph.
 * <p>
 * Uses Dijkstra's alorithm for finding the shortest path.
 * <p>
 * The provided cost function must return a positive value
 * for every arrow in the graph.
 *
 * @param <V> the vertex type
 * @param <A> the arrow type
 * @author Werner Randelshofer
 */
public class AnyDoubleShortestPathBuilder<V, A> extends AbstractDoubleShortestPathBuilder<V, A> {
    public AnyDoubleShortestPathBuilder() {
    }

    public AnyDoubleShortestPathBuilder(@NonNull DirectedGraph<V, A> graph, @NonNull ToDoubleFunction<A> costf) {
        super(graph, costf);
    }

    public AnyDoubleShortestPathBuilder(@NonNull DirectedGraph<V, A> graph, @NonNull ToDoubleTriFunction<V, V, A> costf) {
        super(graph, costf);
    }

    public AnyDoubleShortestPathBuilder(@NonNull Function<V, Iterable<Arc<V, A>>> nextNodesFunction, @NonNull ToDoubleFunction<A> costf) {
        super(nextNodesFunction, costf);
    }

    public AnyDoubleShortestPathBuilder(@NonNull Function<V, Iterable<Arc<V, A>>> nextNodesFunction, @NonNull ToDoubleTriFunction<V, V, A> costf) {
        super(nextNodesFunction, costf);
    }

    /**
     * Searches shortest path using Dijkstra's algorithm.
     * <p>
     * This algorithm does not update an entry in the priority queue.
     * Whenever a distance of a vertex is reduced, we add one more
     * entry to the priority queue. Even if there are multiple entries
     * in the priority queue, we only consider the one with the minimum
     * distance.
     * <p>
     * References:
     * <dl>
     *     <dt>Geeks for Geeks, Dijkstra’s Shortest Path Algorithm using priority_queue of STL</dt>
     *     <dd><a href="https://www.geeksforgeeks.org/dijkstras-shortest-path-algorithm-using-priority_queue-stl/">geeksforgeeks.org</a></dd>
     * </dl>
     */
    public static <V, A> BackLink<V, A> searchShortestPathArcsDouble(@NonNull Iterable<V> starts,
                                                                     @NonNull Predicate<V> goalPredicate,
                                                                     double maxCost,
                                                                     @NonNull Function<V, Iterable<Arc<V, A>>> nextf,
                                                                     @NonNull ToDoubleTriFunction<V, V, A> costf) {
        // Priority queue: back-links with shortest distance from start come first.
        PriorityQueue<MyBackLinkDouble<V, A>> queue = new PriorityQueue<>();

        // Map with best known costs from start to a specific vertex. If an entry is missing, we assume infinity.
        Map<V, Double> costMap = new HashMap<>();

        // Insert start itself in priority queue and initialize its cost as 0.
        for (V start : starts) {
            queue.add(new MyBackLinkDouble<>(start, 0.0, null, null));
            costMap.put(start, 0.0);
        }


        // Loop until we have reached the goal, or queue is exhausted.
        while (!queue.isEmpty()) {
            MyBackLinkDouble<V, A> node = queue.remove();
            final V u = node.vertex;
            if (goalPredicate.test(u)) {
                return node;
            }
            double costToU = node.cost;

            for (Arc<V, A> entry : nextf.apply(u)) {
                V v = entry.getEnd();
                A a = entry.getData();
                double bestKnownCost = costMap.getOrDefault(v, Double.POSITIVE_INFINITY);
                double costThroughU = costToU + costf.applyAsDouble(u, v, a);

                // If there is a shorter path to v through u.
                if (costThroughU < bestKnownCost && costThroughU <= maxCost) {
                    // Update cost to v.
                    costMap.put(v, costThroughU);
                    queue.add(new MyBackLinkDouble<>(v, costThroughU, node, a));
                }
            }
        }

        return null;
    }

    public static <V> BackLink<V, Double> searchShortestPathVerticesDouble(@NonNull Iterable<V> starts,
                                                                           @NonNull Predicate<V> goalPredicate,
                                                                           double maxCost,
                                                                           @NonNull Function<V, Iterable<V>> nextf,
                                                                           @NonNull ToDoubleBiFunction<V, V> costf) {
        // Priority queue: back-links with shortest distance from start come first.
        PriorityQueue<MyBackLinkDouble<V, Double>> queue = new PriorityQueue<>();

        // Map with best known costs from start to a specific vertex. If an entry is missing, we assume infinity.
        Map<V, Double> costMap = new HashMap<>();

        // Insert start itself in priority queue and initialize its cost as 0.
        for (V start : starts) {

            queue.add(new MyBackLinkDouble<>(start, 0.0, null, null));
            costMap.put(start, 0.0);

        }

        // Loop until we have reached the goal, or queue is exhausted.
        while (!queue.isEmpty()) {
            MyBackLinkDouble<V, Double> node = queue.remove();
            final V u = node.vertex;
            if (goalPredicate.test(u)) {
                return node;
            }
            double costToU = node.cost;

            for (V v : nextf.apply(u)) {
                double bestKnownCost = costMap.getOrDefault(v, Double.POSITIVE_INFINITY);
                double costThroughU = costToU + costf.applyAsDouble(u, v);

                // If there is a shorter path to v through u.
                if (costThroughU < bestKnownCost && costThroughU <= maxCost) {
                    // Update cost to v.
                    costMap.put(v, costThroughU);
                    queue.add(new MyBackLinkDouble<>(v, costThroughU, node, costThroughU));
                }
            }
        }

        return null;
    }

    /**
     * Searches shortest path using Dijkstra's algorithm.
     * <p>
     * This algorithm does not update an entry in the priority queue.
     * Whenever a distance of a vertex is reduced, we add one more
     * entry to the priority queue. Even if there are multiple entries
     * in the priority queue, we only consider the one with the minimum
     * distance.
     * <p>
     * References:
     * <dl>
     *     <dt>Geeks for Geeks, Dijkstra’s Shortest Path Algorithm using priority_queue of STL</dt>
     *     <dd><a href="https://www.geeksforgeeks.org/dijkstras-shortest-path-algorithm-using-priority_queue-stl/">geeksforgeeks.org</a></dd>
     * </dl>
     */
    protected @Nullable BackLink<V, A> search(@NonNull V start,
                                              @NonNull Predicate<V> goalPredicate,
                                              double maxCost,
                                              @NonNull Function<V, Iterable<Arc<V, A>>> nextf,
                                              @NonNull ToDoubleTriFunction<V, V, A> costf) {
        return searchShortestPathArcsDouble(Collections.singleton(start),
                goalPredicate, maxCost, nextf, costf);
    }

    protected static class MyBackLinkDouble<VV, AA> extends BackLink<VV, AA> {
        protected final @NonNull VV vertex;
        protected final @Nullable MyBackLinkDouble<VV, AA> parent;
        protected final @Nullable AA arrow;
        protected final double cost;
        protected final int length;

        public MyBackLinkDouble(@NonNull VV node, double cost, @Nullable AnyDoubleShortestPathBuilder.MyBackLinkDouble<VV, AA> parent, @Nullable AA arrow) {
            this.vertex = node;
            this.cost = cost;
            this.parent = parent;
            this.arrow = arrow;
            this.length = parent == null ? 0 : parent.length + 1;
        }

        @Override
        public int compareTo(@NonNull BackLink<VV, AA> that) {
            int result = Double.compare(this.getCost(), that.getCost());
            return result == 0
                    ? Integer.compare(this.length, that.getLength())
                    : result;
        }

        @Override
        public AA getArrow() {
            return arrow;
        }

        public double getCost() {
            return cost;
        }

        @Override
        public long getCostLong() {
            return (long) cost;
        }

        @Override
        public int getLength() {
            return length;
        }

        public @Nullable MyBackLinkDouble<VV, AA> getParent() {
            return parent;
        }

        public @NonNull VV getVertex() {
            return vertex;
        }
    }


}
