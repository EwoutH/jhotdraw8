/*
 * @(#)SelectNothingSelector.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.css.ast;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.css.SelectorModel;

/**
 * A "select nothing selector" matches nothing.
 * <p>
 * This selector is used, when the parser does not understand the
 * syntax of the selector.
 *
 * @author Werner Randelshofer
 */
public class SelectNothingSelector extends SimpleSelector {

    @Override
    public @NonNull String toString() {
        return "SelectNothing";
    }

    @Override
    public @Nullable <T> T match(SelectorModel<T> model, T element) {
        return null;
    }

    @Override
    public int getSpecificity() {
        return 0;
    }
}
