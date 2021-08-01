/*
 * @(#)AbstractTreePresentationModel.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.tree;

import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.beans.NonNullObjectProperty;

/**
 * The {@code TreePresentationModel} can be used to present a {@code TreeModel}
 * in a {@code TreeView} or a {@code TreeTableView}.
 * <p>
 * Maps {@code TreeModel} to a {@code TreeItem&lt;E&gt;} hierarchy.
 * <p>
 * Note: for performance reasons we do not expand the tree nodes by default.
 *
 * @param <N> the node type
 * @author Werner Randelshofer
 */
public abstract class AbstractTreePresentationModel<N> implements TreePresentationModel<N> {
    /**
     * Holds the underlying model.
     */
    private final @Nullable NonNullObjectProperty<TreeModel<N>> treeModel //
            = new NonNullObjectProperty<TreeModel<N>>(this, MODEL_PROPERTY, new SimpleTreeModel<>()) {
        private @Nullable TreeModel<N> oldValue = null;

        @Override
        protected void fireValueChangedEvent() {
            TreeModel<N> newValue = get();
            super.fireValueChangedEvent();
            onTreeModelChanged(oldValue, newValue);
            oldValue = newValue;
        }
    };

    public @Nullable NonNullObjectProperty<TreeModel<N>> treeModelProperty() {
        return treeModel;
    }


    protected abstract void onTreeModelChanged(TreeModel<N> oldValue, TreeModel<N> newValue);
}
