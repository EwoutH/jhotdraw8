/*
 * @(#)PointLocator.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.draw.locator;

import javafx.geometry.Point2D;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.collection.NonNullMapAccessor;
import org.jhotdraw8.css.CssPoint2D;
import org.jhotdraw8.draw.figure.Figure;

/**
 * A {@link Locator} which locates a node on a point of a Figure.
 *
 * @author Werner Randelshofer
 */
public class PointLocator extends AbstractLocator {

    private static final long serialVersionUID = 1L;
    private NonNullMapAccessor<CssPoint2D> key;

    public PointLocator(NonNullMapAccessor<CssPoint2D> key) {
        this.key = key;
    }

    @Override
    public @NonNull Point2D locate(@NonNull Figure owner) {
        return owner.getNonNull(key).getConvertedValue();
    }
}
