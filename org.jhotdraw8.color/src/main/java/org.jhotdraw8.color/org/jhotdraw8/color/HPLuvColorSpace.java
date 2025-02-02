package org.jhotdraw8.color;

import java.awt.color.ColorSpace;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * HPLuv is a human-friendly alternative to HSL.
 * It can only produce pastel colors.
 * <p>
 * References:
 * <dl>
 *     <dt>HSLuv Color Space</dt><dd><a href="https://www.hsluv.org/">www.hsluv.org</a></dd>
 *     <dt>HSLuv Reference Implementation</dt><dd><a href="https://github.com/hsluv/hsluv-java">github.com</a></dd>
 * </dl>
 *
 * @author Werner Randelshofer
 */
public class HPLuvColorSpace extends AbstractNamedColorSpace {
    private static final long serialVersionUID = 1L;

    private static HPLuvColorSpace instance;

    public static HPLuvColorSpace getInstance() {
        if (instance == null) {
            instance = new HPLuvColorSpace();
        }
        return instance;
    }

    public HPLuvColorSpace() {
        super(ColorSpace.TYPE_HSV, 3);
    }

    @Override
    public float[] toRGB(float[] hpluv, float[] sRgb) {
        double[] sRgbD = HUSLColorConverter.hpluvToRgb(new double[]{hpluv[0],
                hpluv[1], hpluv[2]});
        sRgb[0] = (float) sRgbD[0];
        sRgb[1] = (float) sRgbD[1];
        sRgb[2] = (float) sRgbD[2];
        return sRgb;
    }

    @Override
    public float[] fromRGB(float[] sRgb, float[] hpluv) {
        double[] hsluv = HUSLColorConverter.rgbToHpluv(new double[]{sRgb[0], sRgb[1], sRgb[2]});
        hpluv[0] = (float) hsluv[0];
        hpluv[1] = (float) hsluv[1];
        hpluv[2] = (float) hsluv[2];
        return hpluv;
    }

    @Override
    public String getName(int idx) {
        switch (idx) {
        case 0:
            return "Hue";
        case 1:
            return "Saturation";
        case 2:
            return "Lightness";
        default:
            throw new IllegalArgumentException("index must be between 0 and 2:" + idx);
        }
    }

    @Override
    public float getMaxValue(int component) {
        if (component == 0) {
            return 360;
        }
        return 100;
    }

    @Override
    public float getMinValue(int component) {
        return 0f;
    }

    @Override
    public String getName() {
        return "HPLuv";
    }

    private static float clamp(float v, float minv, float maxv) {
        return max(minv, min(v, maxv));
    }
}
