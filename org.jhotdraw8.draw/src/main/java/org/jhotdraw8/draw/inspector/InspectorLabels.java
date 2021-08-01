/*
 * @(#)InspectorLabels.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.draw.inspector;

import org.jhotdraw8.util.Resources;

/**
 * InspectorLabels.
 *
 * @author Werner Randelshofer
 */
public class InspectorLabels {

    public static final String RESOURCE_BUNDLE = "org.jhotdraw8.draw.inspector.Labels";

    public static Resources getResources() {
        return Resources.getResources("org.jhotdraw8.draw", RESOURCE_BUNDLE);
    }
}
