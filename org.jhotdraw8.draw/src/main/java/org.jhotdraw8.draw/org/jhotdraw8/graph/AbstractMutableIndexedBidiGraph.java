package org.jhotdraw8.graph;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.collection.IntArrayList;
import org.jhotdraw8.collection.IntEnumeratorSpliterator;

import java.util.Arrays;
import java.util.Objects;

/**
 * AbstractDirectedGraphBuilder.
 * <p>
 * <b>Implementation:</b>
 * <p>
 * Example graph:
 * <pre>
 *     0 ──→ 1 ──→ 2
 *     │     │
 *     ↓     ↓
 *     3 ←── 4
 * </pre>
 * If the graph is inserted in the following sequence
 * into the builder:
 * <pre>
 *     buildAddVertex();
 *     buildAddVertex();
 *     buildAddVertex();
 *     buildAddVertex();
 *     buildAddVertex();
 *     buildAddVertex();
 *     build.addArrow(0, 1);
 *     build.addArrow(0, 3);
 *     build.addArrow(1, 2);
 *     build.addArrow(1, 4);
 *     build.addArrow(4, 3);
 * </pre>
 * Then the internal representation is as follows:
 * <pre>
 *     vertexCount: 5
 *
 *  vertex#    nodes
 *
 *    0        Node.next{1,3}
 *    1        Node.next{2,4}.prev{0}
 *    2        Node.prev{1}
 *    3        Node.prev{0}.prev{4}
 *    4        Node.next{3}.prev{1}
 * </pre>
 *
 * @author Werner Randelshofer
 */

public abstract class AbstractMutableIndexedBidiGraph implements IndexedBidiGraph {
    private Node[] nodes = new Node[0];
    private int vertexCount;
    private int arrowCount;

    public AbstractMutableIndexedBidiGraph() {
    }

    public AbstractMutableIndexedBidiGraph(int vertexCount) {
        buildAddVertices(vertexCount);
    }

    public AbstractMutableIndexedBidiGraph(@NonNull IndexedDirectedGraph g) {
        buildAddVertices(g.getVertexCount());
        for (int v = 0; v < vertexCount; v++) {
            for (IntEnumeratorSpliterator it = g.getNextVertices(v); it.moveNext(); ) {
                buildAddArrow(v, it.current());
            }
        }
    }

    protected void buildAddVertex() {
        vertexCount++;
        if (nodes.length < vertexCount) {
            nodes = Arrays.copyOf(nodes, vertexCount * 2);
        }
    }

    protected void buildAddVertices(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count: " + count);
        }
        vertexCount += count;
        if (nodes.length < vertexCount) {
            nodes = Arrays.copyOf(nodes, vertexCount * 2);
        }
    }

    protected void buildRemoveVertex(int vidx) {
        Objects.checkIndex(vidx, vertexCount);
        Node vnode = nodes[vidx];
        arrowCount -= vnode.next.size();
        if (vidx < vertexCount - 1) {
            System.arraycopy(nodes, vidx + 1, nodes, vidx, vertexCount - vidx);
        }
        nodes[vertexCount - 1] = null;
        vertexCount--;
        for (int i = 0, nodesLength = nodes.length; i < nodesLength; i++) {
            Node node = nodes[i];
            if (node != null) {
                for (int j = node.next.size() - 1; j >= 0; j--) {
                    int uidx = node.next.get(j);
                    if (uidx == vidx) {
                        node.next.removeAt(j);
                        arrowCount--;
                    } else if (uidx > vidx) {
                        node.next.set(j, uidx - 1);
                    }
                }
                for (int j = node.prev.size() - 1; j >= 0; j--) {
                    int uidx = node.prev.get(j);
                    if (uidx == vidx) {
                        node.prev.removeAt(j);
                    } else if (uidx > vidx) {
                        node.prev.set(j, uidx - 1);
                    }
                }
                if (node.isNodeEmpty()) {
                    nodes[i] = null;
                }
            }
        }
    }

    /**
     * Builder-method: adds a directed arrow from 'v' to 'u'.
     *
     * @param vidx index of v
     * @param uidx index of u
     */
    protected void buildAddArrow(int vidx, int uidx) {
        Node node = nodes[vidx];
        if (node == null) {
            node = nodes[vidx] = new Node();
        }
        node.next.add(uidx);

        node = nodes[uidx];
        if (node == null) {
            node = nodes[uidx] = new Node();
        }
        node.prev.add(vidx);

        arrowCount++;
    }

    /**
     * Removes an arrow from v to u.
     *
     * @param vidx index of v
     * @param uidx index of u
     */
    protected void buildRemoveArrow(int vidx, int uidx) {
        Node node = nodes[vidx];
        if (node == null) {
            throw new IndexOutOfBoundsException(vidx);
        }
        buildRemoveArrowAt(vidx, node.next.indexOf(uidx));
    }

    /**
     * Removes the i-th arrow of vertex v.
     *
     * @param vidx index of v
     * @param i    the i-th arrow of the vertex
     */
    protected void buildRemoveArrowAt(int vidx, int i) {
        Node vnode = nodes[vidx];
        if (vnode == null) {
            throw new IndexOutOfBoundsException(i);
        }
        int uidx = vnode.next.removeAt(i);
        if (vnode.isNodeEmpty()) {
            nodes[vidx] = null;
        }

        Node unode = nodes[uidx];
        if (unode == null) {
            throw new IndexOutOfBoundsException(i);
        }
        unode.prev.removeAt(unode.prev.indexOf(vidx));
        if (unode.isNodeEmpty()) {
            nodes[uidx] = null;
        }
        arrowCount--;
    }

    @Override
    public int getArrowCount() {
        return arrowCount;
    }

    @Override
    public int getNext(int vidx, int k) {
        Node node = nodes[vidx];
        if (node == null) {
            throw new IndexOutOfBoundsException(k);
        }
        return node.next.get(k);
    }

    @Override
    public int getNextCount(int vidx) {
        Node node = nodes[vidx];
        return (node == null) ? 0 : node.next.size();
    }

    @Override
    public int getPrev(int vidx, int i) {
        Node node = nodes[vidx];
        if (node == null) {
            throw new IndexOutOfBoundsException(i);
        }
        return node.prev.get(i);
    }

    @Override
    public int getPrevCount(int vidx) {
        Node node = nodes[vidx];
        return (node == null) ? 0 : node.prev.size();
    }

    @Override
    public int getVertexCount() {
        return vertexCount;
    }

    /**
     * Performance: node.next is node.this. This saves 1 object per node.
     */
    private static class Node extends IntArrayList {
        private final IntArrayList next = this;
        private final IntArrayList prev = new IntArrayList();

        private boolean isNodeEmpty() {
            return next.isEmpty() && prev.isEmpty();
        }
    }
}