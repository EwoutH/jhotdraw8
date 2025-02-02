/*
 * @(#)InteractiveHandleRenderer.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.draw.render;

import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlySetProperty;
import javafx.beans.property.ReadOnlySetWrapper;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.util.Duration;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.base.event.Listener;
import org.jhotdraw8.collection.ReversedList;
import org.jhotdraw8.draw.DrawingEditor;
import org.jhotdraw8.draw.DrawingView;
import org.jhotdraw8.draw.figure.Figure;
import org.jhotdraw8.draw.handle.Handle;
import org.jhotdraw8.draw.handle.HandleType;
import org.jhotdraw8.draw.model.DrawingModel;
import org.jhotdraw8.draw.model.SimpleDrawingModel;
import org.jhotdraw8.fxbase.beans.NonNullObjectProperty;
import org.jhotdraw8.fxbase.tree.TreeModelEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class InteractiveHandleRenderer {
    private static final String DRAWING_VIEW = "drawingView";
    private final @NonNull Group handlesPane = new Group();
    private final @NonNull ObjectProperty<DrawingView> drawingView = new SimpleObjectProperty<>(this, DRAWING_VIEW);
    /**
     * This is the set of handles which are out of sync with their JavaFX node.
     */
    private final Set<Figure> dirtyHandles = new HashSet<>();
    /**
     * The selectedFiguresProperty holds the list of selected figures in the
     * sequence they were selected by the user.
     */
    private final SetProperty<Figure> selectedFigures = new SimpleSetProperty<>(this, DrawingView.SELECTED_FIGURES_PROPERTY, FXCollections.observableSet(new LinkedHashSet<>()));
    private final @NonNull ObjectProperty<DrawingEditor> editor = new SimpleObjectProperty<>(this, DrawingView.EDITOR_PROPERTY, null);
    /**
     * Maps each JavaFX node to a handle in the drawing view.
     */
    private final Map<Node, Handle> nodeToHandleMap = new LinkedHashMap<>();
    private final @NonNull Listener<TreeModelEvent<Figure>> treeModelListener = this::onTreeModelEvent;
    /**
     * The set of all handles which were produced by selected figures.
     */
    private final Map<Figure, List<Handle>> handles = new LinkedHashMap<>();
    private final ObservableSet<Handle> handlesView = FXCollections.observableSet(new LinkedHashSet<>());

    /**
     * Provides a read-only view on the current set of handles.
     */
    private final ReadOnlySetWrapper<Handle> handlesProperty = new ReadOnlySetWrapper<>(this, "handles", FXCollections.unmodifiableObservableSet(handlesView));

    /**
     * The set of all secondary handles. One handle at a time may create
     * secondary handles.
     */
    private final ArrayList<Handle> secondaryHandles = new ArrayList<>();
    private final ObjectProperty<Bounds> clipBounds = new SimpleObjectProperty<>(this, "clipBounds",
            new BoundingBox(0, 0, 800, 600));
    private final @NonNull NonNullObjectProperty<DrawingModel> model //
            = new NonNullObjectProperty<>(this, "model", new SimpleDrawingModel());
    private boolean recreateHandles;
    private boolean handlesAreValid;
    private @Nullable Runnable repainter = null;

    public InteractiveHandleRenderer() {
        handlesPane.setManaged(false);
        handlesPane.setAutoSizeChildren(false);
        model.addListener(this::onDrawingModelChanged);
        clipBounds.addListener(this::onClipBoundsChanged);
        selectedFigures.addListener((SetChangeListener<Figure>) change -> recreateHandles());

    }

    private void onDrawingModelChanged(Observable o, @Nullable DrawingModel oldValue, @Nullable DrawingModel newValue) {
        if (oldValue != null) {
            oldValue.removeTreeModelListener(treeModelListener);
        }
        if (newValue != null) {
            newValue.addTreeModelListener(treeModelListener);
            onRootChanged();
        }
    }


    /**
     * Creates selection handles and adds them to the provided list.
     *
     * @param handles The provided list
     */
    protected void createHandles(@NonNull Map<Figure, List<Handle>> handles) {
        List<Figure> selection = new ArrayList<>(getSelectedFigures());
        if (selection.size() > 1) {
            if (getEditor().getAnchorHandleType() != null) {
                Figure anchor = selection.get(0);
                List<Handle> list = handles.computeIfAbsent(anchor, k -> new ArrayList<>());
                anchor.createHandles(getEditor().getAnchorHandleType(), list);
            }
            if (getEditor().getLeadHandleType() != null) {
                Figure anchor = selection.get(selection.size() - 1);
                List<Handle> list = handles.computeIfAbsent(anchor, k -> new ArrayList<>());
                anchor.createHandles(getEditor().getLeadHandleType(), list);
            }
        }
        HandleType handleType = getEditor().getHandleType();
        if (handleType != null) {
            ArrayList<Handle> list = new ArrayList<>();
            for (Figure figure : selection) {
                figure.createHandles(handleType, list);
            }
            for (Handle h : list) {
                Figure figure = h.getOwner();
                handles.computeIfAbsent(figure, k -> new ArrayList<>()).add(h);
            }
        }
    }


    public @NonNull ObjectProperty<DrawingEditor> editorProperty() {
        return editor;
    }


    @SuppressWarnings("unused")
    public @NonNull ObjectProperty<DrawingView> drawingViewProperty() {
        return drawingView;
    }

    public @Nullable Handle findHandle(double vx, double vy) {
        if (recreateHandles) {
            return null;
        }
        final double tolerance = getEditor().getTolerance();
        for (Map.Entry<Node, Handle> e : new ReversedList<>(new ArrayList<>(nodeToHandleMap.entrySet()))) {
            final Handle handle = e.getValue();
            if (!handle.isSelectable()) {
                continue;
            }
            if (handle.contains(getDrawingViewNonNull(), vx, vy, tolerance)) {
                return handle;
            }
        }
        return null;
    }

    private DrawingView getDrawingViewNonNull() {
        return Objects.requireNonNull(drawingView.get(), "drawingView");
    }

    DrawingEditor getEditor() {
        return editorProperty().get();
    }

    public @NonNull Set<Figure> getFiguresWithCompatibleHandle(@NonNull Collection<Figure> figures, Handle master) {
        validateHandles();
        Map<Figure, Figure> result = new HashMap<>();
        for (Map.Entry<Figure, List<Handle>> entry : handles.entrySet()) {
            if (figures.contains(entry.getKey())) {
                for (Handle h : entry.getValue()) {
                    if (h.isCompatible(master)) {
                        result.put(entry.getKey(), null);
                        break;
                    }
                }
            }
        }
        return result.keySet();
    }

    public Node getNode() {
        return handlesPane;
    }

    ObservableSet<Figure> getSelectedFigures() {
        return selectedFiguresProperty().get();
    }

    public void invalidateHandleNodes() {
        handlesAreValid = false;
        dirtyHandles.addAll(handles.keySet());
    }

    public void invalidateHandles() {
        handlesAreValid = false;
    }

    public void revalidateHandles() {
        invalidateHandles();
        repaint();
    }

    public void jiggleHandles() {
        validateHandles();
        List<Handle> copiedList = handles.values().stream().flatMap(List::stream).collect(Collectors.toList());

        // We scale the handles back and forth.
        double amount = 0.1;
        Transition flash = new Transition() {
            {
                setCycleDuration(Duration.millis(100));
                setCycleCount(2);
                setAutoReverse(true);
            }

            @Override
            protected void interpolate(double frac) {
                for (Handle h : copiedList) {
                    Node node = h.getNode(getDrawingViewNonNull());
                    node.setScaleX(1 + frac * amount);
                    node.setScaleY(1 + frac * amount);
                }
            }
        };
        flash.play();
    }

    public Property<DrawingModel> modelProperty() {
        return model;
    }

    private void onClipBoundsChanged(Observable observable) {
        invalidateHandles();
        repaint();
    }

    private void onFigureRemoved(@NonNull Figure figure) {
        invalidateHandles();
    }

    private void onRootChanged() {
        //clearSelection() // is performed by DrawingView
        repaint();
    }

    private void onSubtreeNodesChanged(@NonNull Figure figure) {
        for (Figure f : figure.preorderIterable()) {
            dirtyHandles.add(f);
        }
    }

    private void onTreeModelEvent(TreeModelEvent<Figure> event) {
        Figure f = event.getNode();
        switch (event.getEventType()) {
        case NODE_ADDED_TO_PARENT:
            break;
        case NODE_REMOVED_FROM_PARENT:
            onFigureRemoved(f);
            break;
        case NODE_ADDED_TO_TREE:
            break;
        case NODE_REMOVED_FROM_TREE:
            break;
        case NODE_CHANGED:
            onNodeChanged(f);
            break;
        case ROOT_CHANGED:
            onRootChanged();
            break;
        case SUBTREE_NODES_CHANGED:
            onSubtreeNodesChanged(f);
            break;
        default:
            throw new UnsupportedOperationException(event.getEventType()
                    + " not supported");
        }
    }

    private void onNodeChanged(Figure f) {
        if (selectedFigures.contains(f)) {
            dirtyHandles.add(f);
            revalidateHandles();
        }
    }

    public void recreateHandles() {
        handlesAreValid = false;
        recreateHandles = true;
        repaint();
    }

    public void repaint() {
        if (repainter == null) {
            repainter = () -> {
                repainter = null;// must be set at the beginning, because we may need to repaint again
                //updateRenderContext();
                validateHandles();
            };
            Platform.runLater(repainter);
        }
    }

    /**
     * The selected figures.
     * <p>
     * Note: The selection is represent by a {@code LinkedHasSet} because the
     * sequence of the selection is important.
     *
     * @return a list of the selected figures
     */
    @NonNull ReadOnlySetProperty<Figure> selectedFiguresProperty() {
        return selectedFigures;
    }

    public void setDrawingView(DrawingView newValue) {
        drawingView.set(newValue);
    }

    private void updateHandles() {
        if (recreateHandles) {
            // FIXME - We create and destroy many handles here!!!
            for (Map.Entry<Figure, List<Handle>> entry : handles.entrySet()) {
                for (Handle h : entry.getValue()) {
                    h.dispose();
                }
            }
            nodeToHandleMap.clear();
            handles.clear();
            handlesPane.getChildren().clear();
            dirtyHandles.clear();

            createHandles(handles);
            handlesView.clear();
            for (List<Handle> value : handles.values()) {
                handlesView.addAll(value);
            }

            recreateHandles = false;


            // Bounds visibleRect = getVisibleRect();

            for (Map.Entry<Figure, List<Handle>> entry : handles.entrySet()) {
                for (Handle handle : entry.getValue()) {
                    Node n = handle.getNode(getDrawingViewNonNull());
                    handle.updateNode(getDrawingViewNonNull());
                    //  if (visibleRect.intersects(n.getBoundsInParent())) {
                    if (nodeToHandleMap.put(n, handle) == null) {
                        handlesPane.getChildren().add(n);
                    }
                    //  }
                }
            }
        } else {
            Figure[] copyOfDirtyHandles = dirtyHandles.toArray(new Figure[0]);
            dirtyHandles.clear();
            for (Figure f : copyOfDirtyHandles) {
                List<Handle> hh = handles.get(f);
                if (hh != null) {
                    for (Handle h : hh) {
                        h.updateNode(getDrawingViewNonNull());
                    }
                }
            }
        }

        for (Handle h : secondaryHandles) {
            h.updateNode(getDrawingViewNonNull());
        }
    }

    /**
     * Validates the handles.
     */
    private void validateHandles() {
        // Validate handles only, if they are invalid/*, and if
        // the DrawingView has a DrawingEditor.*/
        if (!handlesAreValid) {
            handlesAreValid = true;
            updateHandles();
        }
    }

    public void setSelectedFigures(ObservableSet<Figure> selectedFigures) {
        this.selectedFigures.set(selectedFigures);
    }

    public @NonNull ReadOnlySetProperty<Handle> handlesProperty() {
        return handlesProperty.getReadOnlyProperty();
    }
}
