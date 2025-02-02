/*
 * @(#)DefaultableStyleableMapAccessor.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.draw.key;

import org.jhotdraw8.collection.typesafekey.NonNullMapAccessor;
import org.jhotdraw8.css.value.CssDefaultableValue;


public interface DefaultableStyleableMapAccessor<T> extends NonNullMapAccessor<CssDefaultableValue<T>> {
    T getInitialValue();
}
