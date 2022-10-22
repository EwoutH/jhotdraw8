/*
 * @(#)BezierCurves.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.geom;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.base.function.Double2Consumer;
import org.jhotdraw8.base.function.Double4Consumer;
import org.jhotdraw8.base.function.Double6Consumer;
import org.jhotdraw8.base.function.Double8Consumer;
import org.jhotdraw8.collection.OrderedPair;
import org.jhotdraw8.geom.intersect.IntersectRayRay;
import org.jhotdraw8.geom.intersect.IntersectionResultEx;

import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.util.function.ToDoubleFunction;

import static org.jhotdraw8.geom.Geom.lerp;

/**
 * Provides utility methods for Bézier curves.
 *
 * @author Werner Randelshofer
 */
public class BezierCurves {

    /**
     * Prevent instantiation.
     */
    private BezierCurves() {
    }

    /**
     * Evaluates the given curve at the specified time.
     *
     * @param x0 point P0 of the curve
     * @param y0 point P0 of the curve
     * @param x1 point P1 of the curve
     * @param y1 point P1 of the curve
     * @param x2 point P2 of the curve
     * @param y2 point P2 of the curve
     * @param x3 point P3 of the curve
     * @param y3 point P3 of the curve
     * @param t  the time
     * @return the point at time t
     */
    public static @NonNull Point2D.Double evalCubicCurve(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3,
                                                         double t) {
        final double x01, y01, x12, y12, x23, y23, x012, y012, x123, y123, x0123, y0123;
        x01 = lerp(x0, x1, t);
        y01 = lerp(y0, y1, t);

        x12 = lerp(x1, x2, t);
        y12 = lerp(y1, y2, t);

        x23 = lerp(x2, x3, t);
        y23 = lerp(y2, y3, t);

        x012 = lerp(x01, x12, t);
        y012 = lerp(y01, y12, t);

        x123 = lerp(x12, x23, t);
        y123 = lerp(y12, y23, t);

        x0123 = lerp(x012, x123, t);
        y0123 = lerp(y012, y123, t);

        return new Point2D.Double(x0123, y0123);
    }

    /**
     * Evaluates the tangent (that is the first derivative) of the curve at the
     * specified time.
     *
     * @param x0 point P0 of the curve
     * @param y0 point P0 of the curve
     * @param x1 point P1 of the curve
     * @param y1 point P1 of the curve
     * @param x2 point P2 of the curve
     * @param y2 point P2 of the curve
     * @param x3 point P3 of the curve
     * @param y3 point P3 of the curve
     * @param t  the time
     * @return the point at time t
     */
    public static Point2D.Double evalCubicCurveTangent(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3,
                                                       double t) {
        final double x01, y01, x12, y12, x23, y23, x012, y012, x123, y123;
        x01 = lerp(x0, x1, t);
        y01 = lerp(y0, y1, t);

        x12 = lerp(x1, x2, t);
        y12 = lerp(y1, y2, t);

        x23 = lerp(x2, x3, t);
        y23 = lerp(y2, y3, t);

        x012 = lerp(x01, x12, t);
        y012 = lerp(y01, y12, t);

        x123 = lerp(x12, x23, t);
        y123 = lerp(y12, y23, t);

        return new Point2D.Double(x123 - x012, y123 - y012);
    }

    public static @NonNull Point2D.Double evalCubicCurve(CubicCurve2D.Double c,
                                                         double t) {
        return evalCubicCurve(c.x1, c.y1, c.ctrlx1, c.ctrly1, c.ctrlx2, c.ctrly2, c.x2, c.y2, t);
    }


    /**
     * Evaluates the given curve at the specified time.
     *
     * @param x0 point P0 of the curve
     * @param y0 point P0 of the curve
     * @param x1 point P1 of the curve
     * @param y1 point P1 of the curve
     * @param t  the time
     * @return the point at time t
     */
    public static @NonNull Point2D.Double evalLine(double x0, double y0, double x1, double y1, double t) {
        return new Point2D.Double(lerp(x0, x1, t), lerp(y0, y1, t));
    }


    /**
     * Evaluates the given curve at the specified time.
     *
     * @param x0 point P0 of the curve
     * @param y0 point P0 of the curve
     * @param x1 point P1 of the curve
     * @param y1 point P1 of the curve
     * @param x2 point P2 of the curve
     * @param y2 point P2 of the curve
     * @param t  the time
     * @return the point at time t
     */
    public static @NonNull Point2D.Double evalQuadCurve(double x0, double y0, double x1, double y1, double x2, double y2, double t) {
        final double x01, y01, x12, y12, x012, y012;
        x01 = lerp(x0, x1, t);
        y01 = lerp(y0, y1, t);

        x12 = lerp(x1, x2, t);
        y12 = lerp(y1, y2, t);

        x012 = lerp(x01, x12, t);
        y012 = lerp(y01, y12, t);

        return new Point2D.Double(x012, y012);
    }

    public static @NonNull Point2D.Double evalQuadCurveTangent(double x0, double y0, double x1, double y1, double x2, double y2, double t) {
        final double x01, y01, x12, y12;
        x01 = lerp(x0, x1, t);
        y01 = lerp(y0, y1, t);

        x12 = lerp(x1, x2, t);
        y12 = lerp(y1, y2, t);

        return new Point2D.Double(x12 - x01, y12 - y01);
    }

    /**
     * Tries to merge two bézier curves. Returns the new control point.
     *
     * @param x0        point P0 of the first curve
     * @param y0        point P0 of the first curve
     * @param x01       point P1 of the first curve
     * @param y01       point P1 of the first curve
     * @param x012      point P2 of the first curve
     * @param y012      point P2 of the first curve
     * @param x0123     point P3 of the first curve or point p0 of the second curve respectively
     * @param y0123     point P3 of the first curve or point p0 of the second curve respectively
     * @param x123      point P1 of the second curve
     * @param y123      point P1 of the second curve
     * @param x23       point P2 of the second curve
     * @param y23       point P2 of the second curve
     * @param x3        point P3 of the second curve
     * @param y3        point P3 of the second curve
     * @param tolerance distance (radius) at which the joined point may be off from x0123,y0123.
     * @return the control points of the new curve (x0,y0)(x1,y1)(x2,y2)(x3,y3), null if merging failed
     */
    public static @Nullable double[] mergeCubicCurve(final double x0, final double y0, final double x01, final double y01,
                                                     final double x012, final double y012, final double x0123, final double y0123,
                                                     final double x123, final double y123,
                                                     final double x23, final double y23, final double x3, final double y3,
                                                     double tolerance) {

        final double t = (x012 - x123 == 0) ? (y012 - y0123) / (y012 - y123) : (x012 - x0123) / (x012 - x123);
        final Point2D.Double ctrl1, ctrl2;
        if (t == 0 || t == 1) {
            ctrl1 = new Point2D.Double(x01, y01);
            ctrl2 = new Point2D.Double(x23, y23);
        } else {
            ctrl1 = Points2D.add(Points2D.divide(Points2D.subtract(x01, y01, x0, y0), t), x0, y0);
            ctrl2 = Points2D.add(Points2D.divide(Points2D.subtract(x23, y23, x3, y3), 1 - t), x3, y3);
        }

        final Point2D.Double joint0123 = evalCubicCurve(x0, y0, ctrl1.getX(), ctrl1.getY(), ctrl2.getX(), ctrl2.getY(), x3, y3, t);

        return joint0123.distance(x0123, y0123) <= tolerance
                ? new double[]{x0, y0, ctrl1.getX(), ctrl1.getY(), ctrl2.getX(), ctrl2.getY(), x3, y3} : null;
    }

    /**
     * Tries to join two bézier curves. Returns the new control point.
     *
     * @param x0        point P0 of the first curve
     * @param y0        point P0 of the first curve
     * @param x01       point P1 of the first curve
     * @param y01       point P1 of the first curve
     * @param x012      point P2 of the first curve or point p0 of the second curve respectively
     * @param y012      point P2 of the first curve or point p0 of the second curve respectively
     * @param x12       point P1 of the second curve
     * @param y12       point P1 of the second curve
     * @param x2        point P2 of the second curve
     * @param y2        point P2 of the second curve
     * @param tolerance distance (radius) at which the joined point may be off from x012,y012.
     * @return the control points of the new curve (x0,y0)(x1,y1)(x2,y2), null if joining failed
     */
    public static @Nullable double[] mergeQuadCurve(final double x0, final double y0, final double x01, final double y01,
                                                    final double x012, final double y012, final double x12, final double y12, final double x2, final double y2,
                                                    double tolerance) {
        final Point2D.Double start = new Point2D.Double(x0, y0);
        @NonNull Point2D b0 = new Point2D.Double(x2, y2);

        final IntersectionResultEx isect = IntersectRayRay.intersectRayRayEx(start, Points2D.subtract(new Point2D.Double(x01, y01), start),
                b0, Points2D.subtract(new Point2D.Double(x12, y12), b0));
        if (isect.isEmpty()) {
            return null;
        }
        final Point2D.Double ctrl = isect.getLast();

        final double t = start.distance(x01, y01) / start.distance(ctrl);
        final Point2D.Double joint01 = evalQuadCurve(x0, y0, ctrl.getX(), ctrl.getY(), x2, y2, t);

        return (joint01.distance(x012, y012) <= tolerance) ?
                new double[]{x0, y0, ctrl.getX(), ctrl.getY(), x2, y2} : null;
    }

    /**
     * Splits the provided bezier curve into two parts at the specified
     * parameter value {@code t}.
     * <p>
     * Reference:
     * <a href="https://stackoverflow.com/questions/8369488/splitting-a-bezier-curve">splitting-a-bezier-curve</a>.
     */
    public static OrderedPair<CubicCurve2D.Double, CubicCurve2D.Double> split(CubicCurve2D.Double source,
                                                                              double t) {
        CubicCurve2D.Double left = new CubicCurve2D.Double();
        CubicCurve2D.Double right = new CubicCurve2D.Double();
        splitCubicCurve(source.x1, source.y1,
                source.ctrlx1, source.ctrly1,
                source.ctrlx2, source.ctrly2,
                source.x2, source.y2,
                t,
                left::setCurve, right::setCurve);
        return new OrderedPair<>(left, right);
    }

    /**
     * Splits the provided bezier curve into two parts at the specified
     * parameter value {@code t}.
     * <p>
     * Reference:
     * <a href="https://stackoverflow.com/questions/8369488/splitting-a-bezier-curve">splitting-a-bezier-curve</a>.
     */
    public static void split(CubicCurve2D.Double source,
                             double t,
                             CubicCurve2D.Double left,
                             CubicCurve2D.Double right) {
        splitCubicCurve(source.x1, source.y1, source.ctrlx1, source.ctrly1, source.ctrlx2, source.ctrly2, source.x2, source.y2,
                t,

                left::setCurve, right::setCurve);
    }

    /**
     * Splits the provided bezier curve into two parts at the specified
     * parameter value {@code t}.
     * <p>
     * Reference:
     * <a href="https://stackoverflow.com/questions/8369488/splitting-a-bezier-curve">splitting-a-bezier-curve</a>.
     *
     * @param x0    point P0 of the curve
     * @param y0    point P0 of the curve
     * @param x1    point P1 of the curve
     * @param y1    point P1 of the curve
     * @param x2    point P2 of the curve
     * @param y2    point P2 of the curve
     * @param x3    point P3 of the curve
     * @param y3    point P3 of the curve
     * @param t     where to split
     * @param left  if not null, accepts the curve from x1,y1 to t
     * @param right if not null, accepts the curve from t to x4,y4
     */
    public static void splitCubicCurveTo(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3,
                                         double t,
                                         @Nullable Double6Consumer left,
                                         @Nullable Double6Consumer right) {
        splitCubicCurve(x0, y0, x1, y1, x2, y2, x3, y3, t,
                left == null ? null : (lx, ly, lx1, ly1, lx2, ly2, lx3, ly3) -> left.accept(lx1, ly1, lx2, ly2, lx3, ly3),
                right == null ? null : (lx, ly, lx1, ly1, lx2, ly2, lx3, ly3) -> right.accept(lx1, ly1, lx2, ly2, lx3, ly3)
        );
    }

    /**
     * Splits the provided bezier curve into two parts at the specified
     * parameter value {@code t}.
     * <p>
     * Reference:
     * <a href="https://stackoverflow.com/questions/8369488/splitting-a-bezier-curve">splitting-a-bezier-curve</a>.
     *
     * @param x0    point P0 of the curve
     * @param y0    point P0 of the curve
     * @param x1    point P1 of the curve
     * @param y1    point P1 of the curve
     * @param x2    point P2 of the curve
     * @param y2    point P2 of the curve
     * @param x3    point P3 of the curve
     * @param y3    point P3 of the curve
     * @param t     where to split
     * @param left  if not null, accepts the curve from x1,y1 to t
     * @param right if not null, accepts the curve from t to x4,y4
     */
    public static void splitCubicCurve(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3,
                                       double t,
                                       @Nullable Double8Consumer left,
                                       @Nullable Double8Consumer right) {
        final double x01, y01, x12, y12, x23, y23, x012, y012, x123, y123, x0123, y0123;
        x01 = lerp(x0, x1, t);
        y01 = lerp(y0, y1, t);
        x12 = lerp(x1, x2, t);
        y12 = lerp(y1, y2, t);
        x23 = lerp(x2, x3, t);
        y23 = lerp(y2, y3, t);
        x012 = lerp(x01, x12, t);
        y012 = lerp(y01, y12, t);
        x123 = lerp(x12, x23, t);
        y123 = lerp(y12, y23, t);
        x0123 = lerp(x012, x123, t);
        y0123 = lerp(y012, y123, t);

        if (left != null) {
            left.accept(x0, y0, x01, y01, x012, y012, x0123, y0123);
        }
        if (right != null) {
            right.accept(x0123, y0123, x123, y123, x23, y23, x3, y3);
        }
    }

    /**
     * Splits the provided bezier curve into two parts.
     * <p>
     * Reference:
     * <a href="https://stackoverflow.com/questions/8369488/splitting-a-bezier-curve">splitting-a-bezier-curve</a>.
     *
     * @param x0    point P0 of the curve
     * @param y0    point P0 of the curve
     * @param x1    point P1 of the curve
     * @param y1    point P1 of the curve
     * @param x2    point P2 of the curve
     * @param y2    point P2 of the curve
     * @param x3    point P3 of the curve
     * @param y3    point P3 of the curve
     * @param t     where to split
     * @param left  if not null, accepts the curve from x1,y1 to t
     * @param right if not null, accepts the curve from t to x4,y4
     */
    public static void splitCubicCurve(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3, double t,
                                       double[] left,
                                       double[] right) {
        splitCubicCurve(x0, y0, x1, y1, x2, y2, x3, y3, t,
                left == null ? null : (x, y, a, b, c, d, e, f) -> {
                    left[0] = a;
                    left[1] = b;
                    left[2] = c;
                    left[3] = d;
                    left[4] = e;
                    left[5] = f;
                },
                right == null ? null : (x, y, a, b, c, d, e, f) -> {
                    right[0] = a;
                    right[1] = b;
                    right[2] = c;
                    right[3] = d;
                    right[4] = e;
                    right[5] = f;
                });

    }

    /**
     * Splits the provided line into two parts.
     *
     * @param x0          point 1 of the line
     * @param y0          point 1 of the line
     * @param x1          point 2 of the line
     * @param y1          point 2 of the line
     * @param t           where to split
     * @param leftLineTo  if not null, accepts the curve from x1,y1 to t
     * @param rightLineTo if not null, accepts the curve from t to x2,y2
     */
    public static void splitLine(double x0, double y0, double x1, double y1, double t,
                                 Double2Consumer leftLineTo,
                                 Double2Consumer rightLineTo) {
        Geom.splitLine(x0, y0, x1, y1, t, leftLineTo, rightLineTo);
    }

    public static void splitQuadCurveTo(double x0, double y0, double x1, double y1, double x2, double y2, double t,
                                        double[] left,
                                        double[] right) {
        splitQuadCurveTo(x0, y0, x1, y1, x2, y2, t,
                (a, b, c, d) -> {
                    left[0] = a;
                    left[1] = b;
                    left[2] = c;
                    left[3] = d;
                },
                (a, b, c, d) -> {
                    right[0] = a;
                    right[1] = b;
                    right[2] = c;
                    right[3] = d;
                });

    }

    /**
     * Splits the provided bezier curve into two parts.
     *
     * @param x0           point 1 of the curve
     * @param y0           point 1 of the curve
     * @param x1           point 2 of the curve
     * @param y1           point 2 of the curve
     * @param x2           point 3 of the curve
     * @param y2           point 3 of the curve
     * @param t            where to split
     * @param leftCurveTo  if not null, accepts the curve from x1,y1 to t
     * @param rightCurveTo if not null, accepts the curve from t to x3,y3
     */
    public static void splitQuadCurveTo(double x0, double y0, double x1, double y1, double x2, double y2, double t,
                                        @Nullable Double4Consumer leftCurveTo,
                                        @Nullable Double4Consumer rightCurveTo) {
        final double x01, y01, x12, y12, x012, y012;
        x01 = lerp(x0, x1, t);
        y01 = lerp(y0, y1, t);
        x12 = lerp(x1, x2, t);
        y12 = lerp(y1, y2, t);
        x012 = lerp(x01, x12, t);
        y012 = lerp(y01, y12, t);

        if (leftCurveTo != null) {
            leftCurveTo.accept(x01, y01, x012, y012);
        }
        if (rightCurveTo != null) {
            rightCurveTo.accept(x12, y12, x2, y2);
        }
    }

    /**
     * Extracts the specified segment [ta,tb] from the given cubic curve.
     *
     * @param x0 point P0 of the curve
     * @param y0 point P0 of the curve
     * @param x1 point P1 of the curve
     * @param y1 point P1 of the curve
     * @param x2 point P2 of the curve
     * @param y2 point P2 of the curve
     * @param x3 point P3 of the curve
     * @param y3 point P3 of the curve
     * @param ta where to split
     * @param tb where to split
     */
    public static double[] segmentOfCubicCurve(double x0, double y0,
                                               double x1, double y1,
                                               double x2, double y2,
                                               double x3, double y3,
                                               double ta, double tb) {
        double[] left = new double[6];
        double[] right = new double[6];
        double tab = ta / tb;
        splitCubicCurve(x0, y0,
                x1, y1, x2, y2, x3, y3, tb, left, null);
        splitCubicCurve(x0, y0,
                left[0], left[1], left[2], left[3], left[4], left[5], tab, left, right);
        return new double[]{left[4], left[5],
                right[0], right[1], right[2], right[3], right[4], right[5]};
    }

    /**
     * Extracts the specified segment [ta,tb] from the given quad curve.
     *
     * @param x0 point P0 of the curve
     * @param y0 point P0 of the curve
     * @param x1 point P1 of the curve
     * @param y1 point P1 of the curve
     * @param x2 point P2 of the curve
     * @param y2 point P2 of the curve
     * @param ta where to split
     * @param tb where to split
     */
    public static double[] segmentOfQuadCurve(double x0, double y0,
                                              double x1, double y1,
                                              double x2, double y2,
                                              double ta, double tb) {
        double[] left = new double[4];
        double[] right = new double[4];
        double tab = ta / tb;
        splitQuadCurveTo(x0, y0,
                x1, y1, x2, y2, tb, left, null);
        splitQuadCurveTo(x0, y0,
                left[0], left[1], left[2], left[3], tab, left, right);
        return new double[]{left[2], left[3],
                right[0], right[1], right[2], right[3]};
    }


    /**
     * From paper.js, src/path/Curve.js, MIT License.
     *
     * @param v a cubic bezier curve
     * @return the
     */
    public static ToDoubleFunction<Double> getLengthIntegrand(double[] v) {
        // Calculate the coefficients of a Bezier derivative.
        double x0 = v[0], y0 = v[1],
                x1 = v[2], y1 = v[3],
                x2 = v[4], y2 = v[5],
                x3 = v[6], y3 = v[7],

                ax = 9 * (x1 - x2) + 3 * (x3 - x0),
                bx = 6 * (x0 + x2) - 12 * x1,
                cx = 3 * (x1 - x0),

                ay = 9 * (y1 - y2) + 3 * (y3 - y0),
                by = 6 * (y0 + y2) - 12 * y1,
                cy = 3 * (y1 - y0);

        return (t) -> {
            // Calculate quadratic equations of derivatives for x and y
            double dx = (ax * t + bx) * t + cx,
                    dy = (ay * t + by) * t + cy;
            return Math.hypot(dx, dy);
        };
    }
}
