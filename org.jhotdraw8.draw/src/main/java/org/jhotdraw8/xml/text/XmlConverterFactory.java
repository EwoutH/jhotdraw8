/*
 * @(#)XmlConverterFactory.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.xml.text;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.text.Converter;
import org.jhotdraw8.text.ConverterFactory;
import org.jhotdraw8.text.DefaultConverter;
import org.jhotdraw8.text.XmlWordConverter;

/**
 * XmlConverterFactory.
 * <p>
 * Supports the following types:
 * <ul>
 * <li>number</li>
 * <li>word</li>
 * </ul>
 *
 * @author Werner Randelshofer
 */
public class XmlConverterFactory implements ConverterFactory {

    @Override
    public @NonNull Converter<?> apply(@Nullable String type, String style) {
        if (type == null) {
            return new DefaultConverter();
        }
        switch (type) {
        case "number":
            return new XmlNumberConverter();
        case "word":
            return new XmlWordConverter();
        default:
            throw new IllegalArgumentException("illegal type:" + type);
        }
    }

}
