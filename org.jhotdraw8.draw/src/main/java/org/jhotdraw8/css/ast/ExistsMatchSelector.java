/*
 * @(#)ExistsMatchSelector.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.css.ast;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.css.CssToken;
import org.jhotdraw8.css.CssTokenType;
import org.jhotdraw8.css.SelectorModel;

import java.util.function.Consumer;

/**
 * An "exists match" matches an element if the element has an attribute with the
 * specified name.
 *
 * @author Werner Randelshofer
 */
public class ExistsMatchSelector extends AbstractAttributeSelector {
    private final @Nullable String namespace;
    private final @NonNull String attributeName;

    public ExistsMatchSelector(@Nullable String namespace, @NonNull String attributeName) {
        this.namespace = namespace;
        this.attributeName = attributeName;
    }

    @Override
    protected @Nullable <T> T match(@NonNull SelectorModel<T> model, @NonNull T element) {
        return model.hasAttribute(element, namespace, attributeName) ? element : null;
    }

    @Override
    public @NonNull String toString() {
        return "[" + namespace + ":" + attributeName + ']';
    }

    @Override
    public void produceTokens(@NonNull Consumer<CssToken> consumer) {
        consumer.accept(new CssToken(CssTokenType.TT_LEFT_SQUARE_BRACKET));
        if (namespace != null) {
            consumer.accept(new CssToken(CssTokenType.TT_IDENT, namespace));
            consumer.accept(new CssToken(CssTokenType.TT_VERTICAL_LINE));
        }
        consumer.accept(new CssToken(CssTokenType.TT_IDENT, attributeName));
        consumer.accept(new CssToken(CssTokenType.TT_RIGHT_SQUARE_BRACKET));
    }
}
