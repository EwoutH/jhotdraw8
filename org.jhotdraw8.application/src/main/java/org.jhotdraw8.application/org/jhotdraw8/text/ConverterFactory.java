/*
 * @(#)ConverterFactory.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.text;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.base.converter.Converter;

import java.util.function.BiFunction;

/**
 * Creates a {@code Converter} given a type and style.
 * <p>
 * The factory is allowed to return an already existing converter.
 *
 * @author Werner Randelshofer
 */
public interface ConverterFactory extends BiFunction<String, String, Converter<?>> {

    /**
     * Returns a {@code Converter} given a type and a style.
     *
     * @param type  the type, may be null
     * @param style the style, may be null
     * @return the converter
     * @throws IllegalArgumentException if the type or the style are invalid
     */
    @NonNull
    @Override
    Converter<?> apply(@Nullable String type, @Nullable String style);
}
