/*
 * @(#)NullableSvgPathStyleableKey.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.draw.key;

import javafx.scene.shape.PathElement;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.collection.ImmutableList;
import org.jhotdraw8.css.text.CssFXPathElementsConverter;
import org.jhotdraw8.reflect.TypeToken;
import org.jhotdraw8.styleable.WritableStyleableMapAccessor;
import org.jhotdraw8.text.Converter;

/**
 * NullableFXSvgPathStyleableKey.
 *
 * @author Werner Randelshofer
 */
public class NullableFXPathElementsStyleableKey extends AbstractStyleableKey<ImmutableList<PathElement>> implements WritableStyleableMapAccessor<ImmutableList<PathElement>> {

    private static final long serialVersionUID = 1L;

    private final @NonNull Converter<ImmutableList<PathElement>> converter;

    /**
     * Creates a new instance with the specified name and with null as the
     * default value.
     *
     * @param name The name of the key.
     */
    public NullableFXPathElementsStyleableKey(@NonNull String name) {
        this(name, null);
    }


    /**
     * Creates a new instance with the specified name, type token class, default
     * value, and allowing or disallowing null values.
     *
     * @param key          The name of the name. type parameters are given. Otherwise
     *                     specify them in arrow brackets.
     * @param defaultValue The default value.
     */
    public NullableFXPathElementsStyleableKey(@NonNull String key, @Nullable ImmutableList<PathElement> defaultValue) {
        super(null, key, new TypeToken<ImmutableList<PathElement>>() {
        }.getType(), true, defaultValue);

        converter = new CssFXPathElementsConverter(isNullable());
    }

    @Override
    public @NonNull Converter<ImmutableList<PathElement>> getCssConverter() {
        return converter;
    }
}