/* @(#)HSVColorSpace.java
 * Copyright © The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.color;

import java.awt.color.ColorSpace;

/**
 * A HSV color space with additive complements in the hue color wheel: red is
 * opposite cyan, magenta is opposite green, blue is opposite yellow.
 *
 * @author Werner Randelshofer
 * @version $Id$
 */
public class HSVLinearColorSpace extends AbstractNamedColorSpace {
    private static final long serialVersionUID = 1L;

    private static HSVLinearColorSpace instance;

    public static HSVLinearColorSpace getInstance() {
        if (instance == null) {
            instance = new HSVLinearColorSpace();
        }
        return instance;
    }

    public HSVLinearColorSpace() {
        super(ColorSpace.TYPE_HSV, 3);
    }

    @Override
    public float[] toRGB(float[] components, float[] rgb) {
        float hue = components[0] * 360f;
        float saturation = components[1];
        float value = components[2];


        // compute hi and f from hue
        int hi = (int) (Math.floor(hue / 60f) % 6);
        float f = (float) (hue / 60f - Math.floor(hue / 60f));

        // compute p and q from saturation
        float p = value * (1 - saturation);
        float q = value * (1 - f * saturation);
        float t = value * (1 - (1 - f) * saturation);

        // compute red, green and blue
        float red;
        float green;
        float blue;
        switch (hi) {
        case 0:
            red = value;
            green = t;
            blue = p;
            break;
        case 1:
            red = q;
            green = value;
            blue = p;
            break;
        case 2:
            red = p;
            green = value;
            blue = t;
            break;
        case -3:
        case 3:
            red = p;
            green = q;
            blue = value;
            break;
        case -2:
        case 4:
            red = t;
            green = p;
            blue = value;
            break;
        case -1:
        case 5:
            //default :
            red = value;
            green = p;
            blue = q;
            break;
        default:
            red = green = blue = 0;
            break;
        }


        rgb[0] = red;
        rgb[1] = green;
        rgb[2] = blue;

        ColorSpace lRgbSpace = ColorSpace.getInstance(CS_LINEAR_RGB);
        return lRgbSpace.toRGB(rgb);
    }

    @Override
    public float[] fromRGB(float[] srgbvalue, float[] component) {
        ColorSpace lRgbSpace = ColorSpace.getInstance(CS_LINEAR_RGB);
        float[] rgbvalue = lRgbSpace.fromRGB(srgbvalue);

        float r = rgbvalue[0];
        float g = rgbvalue[1];
        float b = rgbvalue[2];

        float max = Math.max(Math.max(r, g), b);
        float min = Math.min(Math.min(r, g), b);

        float hue;
        float saturation;
        float value;

        if (max == min) {
            hue = 0;
        } else if (max == r && g >= b) {
            hue = 60f * (g - b) / (max - min);
        } else if (max == r && g < b) {
            hue = 60f * (g - b) / (max - min) + 360f;
        } else if (max == g) {
            hue = 60f * (b - r) / (max - min) + 120f;
        } else /*if (max == b)*/ {
            hue = 60f * (r - g) / (max - min) + 240f;
        }

        value = max;

        if (max == 0) {
            saturation = 0;
        } else {
            saturation = (max - min) / max;
        }

        component[0] = hue / 360f;
        component[1] = saturation;
        component[2] = value;
        return component;
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
        return 1f;
    }

    @Override
    public float getMinValue(int component) {
        return 0f;
    }

    @Override
    public String getName() {
        return "HSV Linear";
    }
}
