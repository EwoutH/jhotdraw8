/*
 * @(#)Point2DListStyleableKey.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.draw.key;

import javafx.geometry.Point2D;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.collection.PersistentList;
import org.jhotdraw8.collection.WrappedPersistentList;
import org.jhotdraw8.collection.key.NonNullMapAccessor;
import org.jhotdraw8.css.text.CssPersistentListConverter;
import org.jhotdraw8.css.text.Point2DConverter;
import org.jhotdraw8.reflect.TypeToken;
import org.jhotdraw8.styleable.WritableStyleableMapAccessor;
import org.jhotdraw8.text.Converter;

/**
 * Point2DListStyleableKey.
 *
 * @author Werner Randelshofer
 */
public class Point2DListStyleableKey extends AbstractStyleableKey<@NonNull PersistentList<@NonNull Point2D>>
        implements WritableStyleableMapAccessor<@NonNull PersistentList<@NonNull Point2D>>, NonNullMapAccessor<PersistentList<@NonNull Point2D>> {

    private static final long serialVersionUID = 1L;

    private final @NonNull Converter<PersistentList<@NonNull Point2D>> converter;

    /**
     * Creates a new instance with the specified name and with null as the
     * default value.
     *
     * @param name The name of the key.
     */
    public Point2DListStyleableKey(@NonNull String name) {
        this(name, WrappedPersistentList.emptyList());
    }

    /**
     * Creates a new instance with the specified name, mask and default value.
     *
     * @param name         The name of the key.
     * @param defaultValue The default value.
     */
    public Point2DListStyleableKey(@NonNull String name, @NonNull PersistentList<@NonNull Point2D> defaultValue) {
        super(name, new TypeToken<PersistentList<Point2D>>() {
        }, defaultValue);

        this.converter = new CssPersistentListConverter<>(
                new Point2DConverter(false, false), ", ");
    }

    @Override
    public @NonNull Converter<PersistentList<Point2D>> getCssConverter() {
        return converter;
    }

}
