/*
 * @(#)BlendModeStyleableKey.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.draw.key;

import javafx.scene.effect.BlendMode;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.css.text.CssEnumConverter;
import org.jhotdraw8.styleable.WritableStyleableMapAccessor;
import org.jhotdraw8.text.Converter;

/**
 * BlendModeStyleableKey.
 *
 * @author Werner Randelshofer
 */
public class BlendModeStyleableKey extends AbstractStyleableKey<BlendMode> implements WritableStyleableMapAccessor<BlendMode> {

    static final long serialVersionUID = 1L;


    /**
     * Creates a new instance with the specified name and with null as the
     * default value.
     *
     * @param name The name of the key.
     */
    public BlendModeStyleableKey(@NonNull String name) {
        this(name, null);
    }

    /**
     * Creates a new instance with the specified name and default value.
     *
     * @param name         The name of the key.
     * @param defaultValue The default value.
     */
    public BlendModeStyleableKey(@NonNull String name, BlendMode defaultValue) {
        super(name, BlendMode.class, defaultValue);
    }

    private Converter<BlendMode> converter = new CssEnumConverter<>(BlendMode.class, false);
    ;

    @Override
    public @NonNull Converter<BlendMode> getCssConverter() {
        return converter;
    }

}
