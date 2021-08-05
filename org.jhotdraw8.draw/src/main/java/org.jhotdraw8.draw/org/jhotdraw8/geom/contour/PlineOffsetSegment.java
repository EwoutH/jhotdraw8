/*
 * @(#)PlineOffsetSegment.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.geom.contour;


import java.awt.geom.Point2D;

/**
 * Represents a raw polyline offset segment.
 * <p>
 * This code has been derived from Cavalier Contours [1].
 * <p>
 * References:
 * <dl>
 *     <dt>[1] Cavalier Contours</dt>
 *     <dd>Cavalier Contours, Copyright (c) 2019 Jedidiah Buck McCready, MIT License.
 *     <a href="https://github.com/jbuckmccready/CavalierContours">github.com</a></dd>
 * </dl>
 */
class PlineOffsetSegment {
    final PlineVertex v1;
    final PlineVertex v2;
    final Point2D.Double origV2Pos;
    final boolean collapsedArc;


    public PlineOffsetSegment(PlineVertex v1, PlineVertex v2, Point2D.Double origV2Pos, boolean collapsedArc) {
        this.v1 = v1;
        this.v2 = v2;
        this.origV2Pos = origV2Pos;
        this.collapsedArc = collapsedArc;
    }

    @Override
    public String toString() {
        return "PlineOffsetSegment{" +
                "v1=" + v1 +
                ", v2=" + v2 +
                ", origV2Pos=" + origV2Pos +
                ", collapsedArc=" + collapsedArc +
                '}';
    }
}
