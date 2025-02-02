/*
 * @(#)CssRectangle2DStyleableMapAccessor.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.draw.key;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.base.converter.Converter;
import org.jhotdraw8.collection.typesafekey.Key;
import org.jhotdraw8.collection.typesafekey.NonNullMapAccessor;
import org.jhotdraw8.css.value.CssSize;
import org.jhotdraw8.draw.css.CssRectangle2D;
import org.jhotdraw8.draw.css.converter.CssRectangle2DConverter;

import java.util.Map;
import java.util.Objects;

/**
 * Rectangle2DStyleableMapAccessor.
 *
 * @author Werner Randelshofer
 */
public class CssRectangle2DStyleableMapAccessor extends AbstractStyleableMapAccessor<@NonNull CssRectangle2D>
        implements NonNullMapAccessor<@NonNull CssRectangle2D> {

    private static final long serialVersionUID = 1L;

    private final @NonNull NonNullMapAccessor<CssSize> xKey;
    private final @NonNull NonNullMapAccessor<CssSize> yKey;
    private final @NonNull NonNullMapAccessor<CssSize> widthKey;
    private final @NonNull NonNullMapAccessor<CssSize> heightKey;

    /**
     * Creates a new instance with the specified name.
     *
     * @param name      the name of the accessor
     * @param xKey      the key for the x coordinate of the rectangle
     * @param yKey      the key for the y coordinate of the rectangle
     * @param widthKey  the key for the width of the rectangle
     * @param heightKey the key for the height of the rectangle
     */
    public CssRectangle2DStyleableMapAccessor(String name, @NonNull NonNullMapAccessor<CssSize> xKey, @NonNull NonNullMapAccessor<CssSize> yKey, @NonNull NonNullMapAccessor<CssSize> widthKey, @NonNull NonNullMapAccessor<CssSize> heightKey) {
        super(name, CssRectangle2D.class, new NonNullMapAccessor<?>[]{xKey, yKey, widthKey, heightKey}, new CssRectangle2D(
                xKey.getDefaultValueNonNull(),
                yKey.getDefaultValueNonNull(),
                widthKey.getDefaultValueNonNull(),
                heightKey.getDefaultValueNonNull()));

        this.xKey = xKey;
        this.yKey = yKey;
        this.widthKey = widthKey;
        this.heightKey = heightKey;
    }

    private final Converter<CssRectangle2D> converter = new CssRectangle2DConverter(false);

    @Override
    public @NonNull Converter<CssRectangle2D> getCssConverter() {
        return converter;
    }

    @Override
    public @NonNull CssRectangle2D get(@NonNull Map<? super Key<?>, Object> a) {
        return new CssRectangle2D(xKey.get(a),
                yKey.get(a),
                widthKey.get(a),
                heightKey.get(a));
    }

    @Override
    public void set(@NonNull Map<? super Key<?>, Object> a, @Nullable CssRectangle2D value) {
        Objects.requireNonNull(value, "value");
        xKey.put(a, value.getMinX());
        yKey.put(a, value.getMinY());
        widthKey.put(a, value.getWidth());
        heightKey.put(a, value.getHeight());
    }

    @Override
    public @NonNull CssRectangle2D remove(@NonNull Map<? super Key<?>, Object> a) {
        CssRectangle2D oldValue = get(a);
        xKey.remove(a);
        yKey.remove(a);
        widthKey.remove(a);
        heightKey.remove(a);
        return oldValue;
    }

}
