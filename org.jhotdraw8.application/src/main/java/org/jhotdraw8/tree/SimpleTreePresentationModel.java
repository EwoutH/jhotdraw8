/*
 * @(#)SimpleTreePresentationModel.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.tree;

import javafx.scene.control.TreeItem;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.event.Listener;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * This model can be used to present a {@code TreeModel}
 * in a {@code TreeView} or a {@code TreeTableView}.
 * <p>
 * Maps {@code TreeModel} to a {@code TreeItem&lt;E&gt;} hierarchy.
 * <p>
 * Note: for performance reasons we do not expand the tree nodes by default.
 *
 * @author Werner Randelshofer
 */
public class SimpleTreePresentationModel<N> extends AbstractTreePresentationModel<N> {

    private final Map<N, TreeItem<N>> items = new HashMap<>();
    private final Listener<TreeModelEvent<N>> modelHandler = new Listener<TreeModelEvent<N>>() {
        @Override
        public void handle(@NonNull TreeModelEvent<N> event) {
            updating++;
            try {
                N f = event.getNode();
                switch (event.getEventType()) {
                case NODE_ADDED_TO_PARENT:
                    onNodeAdded(f, event.getParent(), event.getIndex());
                    break;
                case NODE_REMOVED_FROM_PARENT:
                    onNodeRemoved(f, event.getParent(), event.getIndex());
                    break;
                case NODE_ADDED_TO_TREE:
                    onNodeAddedToTree(f, event.getParent(), event.getIndex());
                    break;
                case NODE_REMOVED_FROM_TREE:
                    onNodeRemovedFromTree(f);
                    break;
                case NODE_CHANGED:
                    onNodeInvalidated(f);
                    break;
                case ROOT_CHANGED:
                    onRootChanged();
                    break;
                case SUBTREE_NODES_CHANGED:
                    break;
                default:
                    throw new UnsupportedOperationException(event.getEventType()
                            + " not supported");
                }
            } finally {
                updating--;
            }
        }
    };

    private boolean reversed = true;
    private final TreeItem<N> root = new TreeItem<>();

    protected int updating;

    @Override
    public @NonNull TreeItem<N> getRoot() {
        return root;
    }

    @Override
    public TreeItem<N> getTreeItem(N f) {
        return items.get(f);
    }

    public N getValue(@NonNull TreeItem<N> item) {
        return item.getValue();
    }

    protected void onNodeAdded(N node, N parentE, int index) {
        TreeItem<N> item = items.computeIfAbsent(node, TreeItem::new);
        TreeItem<N> parentItem = items.get(parentE);
        if (reversed) {
            parentItem.getChildren().add(parentItem.getChildren().size() - index, item);
        } else {
            parentItem.getChildren().add(index, item);
        }
    }

    protected void onNodeAddedToTreeOld(N node, N parent, int index) {
        TreeModel<N> m = getTreeModel();
        TreeItem<N> item = new TreeItem<>(node);
        item.setExpanded(false);
        items.put(node, item);
        int childIndex = 0;
        for (int i = 0, n = m.getChildCount(node); i < n; i++) {
            N child = m.getChild(node, i);
            onNodeAddedToTree(child, node, childIndex);
            onNodeAdded(child, node, childIndex);
            childIndex++;
        }
    }

    protected void onNodeAddedToTree(N node, N parent, int index) {
        TreeModel<N> m = getTreeModel();
        TreeItem<N> item = new TreeItem<>(node);
        item.setExpanded(false);
        items.put(node, item);
        int childIndex = 0;
        Deque<TreeItem<N>> deque = new ArrayDeque<>();
        for (int i = 0, n = m.getChildCount(node); i < n; i++) {
            N child = m.getChild(node, i);
            onNodeAddedToTree(child, node, childIndex);
            // Performance: this is extremely slow
            TreeItem<N> childItem = items.computeIfAbsent(child, TreeItem::new);
            if (reversed) {
                deque.addFirst(childItem);
            } else {
                deque.addLast(childItem);
            }
            childIndex++;
        }
        // instead of calling onNodeAdded for every child, we this instead
        item.getChildren().addAll(deque);
    }

    protected void onNodeInvalidated(N f) {
        TreeItem<N> node = items.get(f);
        if (node != null) {
            node.setValue(f);
        }
    }

    protected void onNodeRemoved(N f, N parentE, int index) {
        TreeItem<N> parent = items.get(parentE);
        if (reversed) {
            parent.getChildren().remove(parent.getChildren().size() - 1 - index);
        } else {
            parent.getChildren().remove(index);
        }
    }

    protected void onNodeRemovedFromTree(N f) {
        items.remove(f);
    }

    protected void onRootChanged() {
        TreeModel<N> m = getTreeModel();
        N drawing = m.getRoot();
        root.setValue(drawing);
        root.getChildren().clear();
        items.clear();
        items.put(drawing, root);
        int childIndex = 0;
        if (drawing != null) {
            for (int i = 0, n = m.getChildCount(drawing); i < n; i++) {
                N child = m.getChild(drawing, i);
                onNodeAddedToTree(child, drawing, childIndex);
                onNodeAdded(child, drawing, childIndex);
                childIndex++;
            }
        }
    }

    @Override
    protected void onTreeModelChanged(@Nullable TreeModel<N> oldValue, @NonNull TreeModel<N> newValue) {
        if (oldValue != null) {
            oldValue.removeTreeModelListener(modelHandler);
        }
        newValue.addTreeModelListener(modelHandler);
        onRootChanged();
    }

    @Override
    public boolean isUpdating() {
        return updating > 0;
    }

}
