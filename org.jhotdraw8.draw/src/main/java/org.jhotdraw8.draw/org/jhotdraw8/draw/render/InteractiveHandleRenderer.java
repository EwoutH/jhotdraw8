/*
 * @(#)InteractiveHandleRenderer.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.draw.render;

import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlySetProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.Region;
import javafx.scene.shape.Shape;
import javafx.util.Duration;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.beans.NonNullObjectProperty;
import org.jhotdraw8.collection.ReversedList;
import org.jhotdraw8.draw.DrawingEditor;
import org.jhotdraw8.draw.DrawingView;
import org.jhotdraw8.draw.figure.Figure;
import org.jhotdraw8.draw.handle.Handle;
import org.jhotdraw8.draw.handle.HandleType;
import org.jhotdraw8.draw.model.DrawingModel;
import org.jhotdraw8.draw.model.DrawingModelEvent;
import org.jhotdraw8.draw.model.SimpleDrawingModel;
import org.jhotdraw8.event.Listener;
import org.jhotdraw8.geom.FXGeom;
import org.jhotdraw8.geom.FXShapes;
import org.jhotdraw8.geom.FXTransforms;
import org.jhotdraw8.geom.Geom;
import org.jhotdraw8.tree.TreeModelEvent;

import java.awt.BasicStroke;
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
    private static final String RENDER_CONTEXT = "renderContenxt";
    private static final String DRAWING_VIEW = "drawingView";
    private static final double LINE45DEG = Math.sqrt(0.5);
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
    private final SetProperty<Figure> selectedFigures = new SimpleSetProperty<>(this, DrawingView.SELECTED_FIGURES_PROPERTY, FXCollections.observableSet(new LinkedHashSet<Figure>()));
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
    /**
     * The set of all secondary handles. One handle at a time may create
     * secondary handles.
     */
    private final ArrayList<Handle> secondaryHandles = new ArrayList<>();
    private final ObjectProperty<Bounds> clipBounds = new SimpleObjectProperty<>(this, "clipBounds",
            new BoundingBox(0, 0, 800, 600));
    private final @NonNull NonNullObjectProperty<DrawingModel> model //
            = new NonNullObjectProperty<>(this, "model", new SimpleDrawingModel());
    private NonNullObjectProperty<RenderContext> renderContext = new NonNullObjectProperty<>(this, RENDER_CONTEXT, new SimpleRenderContext());
    private boolean recreateHandles;
    private boolean handlesAreValid;
    private @Nullable Runnable repainter = null;

    public InteractiveHandleRenderer() {
        handlesPane.setManaged(false);
        handlesPane.setAutoSizeChildren(false);
        model.addListener(this::onDrawingModelChanged);
        clipBounds.addListener(this::onClipBoundsChanged);
        selectedFigures.addListener(new SetChangeListener<Figure>() {
            @Override
            public void onChanged(Change<? extends Figure> change) {
                recreateHandles();
            }
        });

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
     * Returns true if the node contains the specified point within a
     * tolerance.
     *
     * @param node         The node
     * @param pointInLocal The point in local coordinates
     * @param tolerance    The maximal distance the point is allowed to be away
     *                     from the node, in local coordinates
     * @return a distance if the node contains the point, null otherwise
     */
    public static Double contains(@NonNull Node node, @NonNull Point2D pointInLocal, double tolerance) {
        double toleranceInLocal = tolerance / FXTransforms.deltaTransform(node.getLocalToSceneTransform(), LINE45DEG, LINE45DEG).magnitude();

        if (!node.isVisible()) {
            return null;
        }

        // If the node has a clip, we only proceed if the point is inside
        // the clip with tolerance.
        final Node nodeClip = node.getClip();
        if (nodeClip instanceof Shape) {
            final java.awt.Shape shape = FXShapes.awtShapeFromFX((Shape) nodeClip);
            if (!shape.intersects(pointInLocal.getX() - toleranceInLocal,
                    pointInLocal.getY() - toleranceInLocal, toleranceInLocal * 2, toleranceInLocal * 2)) {
                return null;
            }
        }


        if (node instanceof Shape) {
            Shape shape = (Shape) node;

            if (shape.contains(pointInLocal)) {
                return 0.0;
            }

            double widthFactor;
            switch (shape.getStrokeType()) {
                case CENTERED:
                default:
                    widthFactor = 0.5;
                    break;
                case INSIDE:
                    widthFactor = 0;
                    break;
                case OUTSIDE:
                    widthFactor = 1;
                    break;
            }
            if (FXGeom.contains(shape.getBoundsInLocal(), pointInLocal, toleranceInLocal)) {
                int cap;
                switch (shape.getStrokeLineCap()) {
                case SQUARE:
                    cap = BasicStroke.CAP_SQUARE;
                    break;
                case BUTT:
                    cap = (toleranceInLocal > 0) ? BasicStroke.CAP_ROUND : BasicStroke.CAP_BUTT;
                    break;
                case ROUND:
                    cap = BasicStroke.CAP_ROUND;
                    break;
                default:
                    throw new IllegalArgumentException();
                }
                int join;
                switch (shape.getStrokeLineJoin()) {
                    case MITER:
                        join = BasicStroke.JOIN_MITER;
                        break;
                    case BEVEL:
                        join = BasicStroke.JOIN_BEVEL;
                        break;
                    case ROUND:
                        join = BasicStroke.JOIN_ROUND;
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                final java.awt.Shape awtShape = FXShapes.awtShapeFromFX(shape);
                return new BasicStroke(2f * (float) (shape.getStrokeWidth() * widthFactor + toleranceInLocal),
                        cap, join, (float) shape.getStrokeMiterLimit()
                ).createStrokedShape(awtShape)
                        .contains(new java.awt.geom.Point2D.Double(pointInLocal.getX(), pointInLocal.getY()))
                        ? Geom.distanceFromShape(awtShape, pointInLocal.getX(), pointInLocal.getY()) : null;
            } else {
                return null;
            }
        } else if (node instanceof Group) {
            if (FXGeom.contains(node.getBoundsInLocal(), pointInLocal, toleranceInLocal)) {
                return childContains((Parent) node, pointInLocal, tolerance);

            }
            return null;
        } else if (node instanceof Region) {
            if (FXGeom.contains(node.getBoundsInLocal(), pointInLocal, toleranceInLocal)) {
                Region region = (Region) node;
                final Background bg = region.getBackground();
                final Border border = region.getBorder();
                if ((bg == null || bg.isEmpty()) && (border == null || border.isEmpty())) {
                    return childContains((Parent) node, pointInLocal, tolerance);
                } else {
                    return 0.0;
                }
            }
            return null;
        } else { // foolishly assumes that all other nodes are rectangular
            return FXGeom.contains(node.getBoundsInLocal(), pointInLocal, tolerance) ? 0.0 : null;
        }
    }

    private static @Nullable Double childContains(final @NonNull Parent node, final @NonNull Point2D pointInLocal, final double tolerance) {
        double minDistance = Double.POSITIVE_INFINITY;
        for (Node child : node.getChildrenUnmodifiable()) {
            Double distance = contains(child, child.parentToLocal(pointInLocal), tolerance);
            if (distance != null) {
                minDistance = Math.min(minDistance, distance);
            }
        }
        return Double.isFinite(minDistance) ? minDistance : null;
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
        ArrayList<Handle> list = new ArrayList<>();
        for (Figure figure : selection) {
            figure.createHandles(handleType, list);
        }
        for (Handle h : list) {
            Figure figure = h.getOwner();
            handles.computeIfAbsent(figure, k -> new ArrayList<>()).add(h);
        }
    }


    public @NonNull ObjectProperty<DrawingEditor> editorProperty() {
        return editor;
    }


    public @NonNull ObjectProperty<DrawingView> drawingViewProperty() {
        return drawingView;
    }

    public @Nullable Handle findHandle(double vx, double vy) {
        if (recreateHandles) {
            return null;
        }
        final double tolerance = getEditor().getTolerance();
        for (Map.Entry<Node, Handle> e : new ReversedList<>(nodeToHandleMap.entrySet())) {
            final Node node = e.getKey();
            final Handle handle = e.getValue();
            if (!handle.isSelectable()) {
                continue;
            }
            if (handle.contains(getDrawingViewNonNull(), vx, vy, tolerance)) {
                return handle;
            } else {
                if (false) {
                    if (contains(node, new Point2D(vx, vy), tolerance) != null) {
                        return handle;
                    }
                }
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

    private void invalidateFigureNode(Figure f) {
        if (handles.containsKey(f)) {
            handlesAreValid = false;
            dirtyHandles.add(f);
        }
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

    private void onDrawingModelChanged(@Nullable DrawingModel oldValue, @Nullable DrawingModel newValue) {
        if (oldValue != null) {
            oldValue.removeTreeModelListener(treeModelListener);
        }
        if (newValue != null) {
            newValue.addTreeModelListener(treeModelListener);
            onRootChanged();
        }
    }

    private void onDrawingModelEvent(DrawingModelEvent drawingModelEvent) {
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

    private void onVisibleRectChanged(Observable o) {
        invalidateHandles();
        repaint();
    }

    private void onZoomFactorChanged(ObservableValue<? extends Number> observable, Number oldValue, @NonNull Number newValue) {
        invalidateHandleNodes();
        repaint();
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

    private void updateLayout() {
        invalidateHandleNodes();
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


}
