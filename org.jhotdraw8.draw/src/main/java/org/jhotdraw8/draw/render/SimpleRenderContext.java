/*
 * @(#)SimpleRenderContext.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.draw.render;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.collection.Key;
import org.jhotdraw8.collection.MapAccessor;
import org.jhotdraw8.collection.NonNullMapAccessor;
import org.jhotdraw8.draw.figure.Figure;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SimpleRenderContext implements WritableRenderContext {
    private @NonNull Map<Figure, Node> nodeMap = new HashMap<>();
    private @NonNull ObservableMap<Key<?>, Object> properties = FXCollections.observableHashMap();

    @Override
    public Node getNode(Figure figure) {
        return nodeMap.computeIfAbsent(figure, f -> f.createNode(this));
    }

    public @NonNull ObservableMap<Key<?>, Object> getProperties() {
        return properties;
    }

    public <T> void set(@NonNull MapAccessor<T> key, @Nullable T value) {
        key.set(properties, value);
    }

    /**
     * Gets a property value.
     *
     * @param <T> the value type
     * @param key the key
     * @return the value
     */
    @Override
    public @Nullable <T> T get(@NonNull MapAccessor<T> key) {
        return key.get(getProperties());
    }

    /**
     * Gets a nonnull property value.
     *
     * @param <T> the value type
     * @param key the key
     * @return the value
     */
    @Override
    public @NonNull <T> T getNonNull(@NonNull NonNullMapAccessor<T> key) {
        T value = key.get(getProperties());
        return Objects.requireNonNull(value);
    }

}
