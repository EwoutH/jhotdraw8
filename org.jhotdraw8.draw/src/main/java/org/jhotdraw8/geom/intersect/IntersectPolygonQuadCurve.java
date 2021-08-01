/*
 * @(#)IntersectPolygonQuadCurve.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.geom.intersect;

import org.jhotdraw8.annotation.NonNull;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The code of this class has been derived from intersection.js [1].
 * <p>
 * References:
 * <dl>
 *     <dt>[1] intersection.js</dt>
 *     <dd>intersection.js, Copyright (c) 2002 Kevin Lindsey, BSD 3-clause license.
 *     <a href="http://www.kevlindev.com/gui/math/intersection/Intersection.js">kevlindev.com</a></dd>
 * </dl>
 */
public class IntersectPolygonQuadCurve {
    /**
     * Computes the intersection between quadratic bezier curve 'p' and the
     * given closed polygon.
     * <p>
     * The intersection will contain the parameters 't' of curve 'a' in range
     * [0,1].
     *
     * @param p0     control point P0 of 'p'
     * @param p1     control point P1 of 'p'
     * @param p2     control point P2 of 'p'
     * @param points the points of the polygon
     * @return the computed intersection
     */
    public static @NonNull IntersectionResult intersectQuadCurvePolygon(@NonNull Point2D p0, @NonNull Point2D p1, @NonNull Point2D p2, @NonNull List<Point2D.Double> points) {
        List<IntersectionPoint> result = new ArrayList<>();
        int length = points.size();

        for (int i = 0; i < length; i++) {
            final Point2D.Double a0, a1;
            a0 = points.get(i);
            a1 = points.get((i + 1) % length);
            IntersectionResult inter = IntersectLineQuadCurve.intersectQuadCurveLine(p0, p1, p2, a0, a1);

            result.addAll(inter.asList());
        }

        return new IntersectionResult(result);
    }
}
