/*
 * @(#)PlineIntersect.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.geom.contour;


import java.awt.geom.Point2D;

/**
 * Represents a non-coincident poly-arc-line intersection.
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
public class PlineIntersect {
    /**
     * Index of the start vertex of the first segment.
     */
    int sIndex1;
    /**
     * Index of the start vertex of the second segment.
     */
    int sIndex2;
    /**
     * Point of intersection.
     */
    Point2D.Double pos;

    PlineIntersect() {
    }

    PlineIntersect(int si1, int si2, Point2D.Double p) {
        this.sIndex1 = si1;
        this.sIndex2 = si2;
        this.pos = p;
    }

    @Override
    public String toString() {
        return "PlineIntersect{" +
                "sIndex1=" + sIndex1 +
                ", sIndex2=" + sIndex2 +
                ", pos=" + pos +
                '}';
    }
}
