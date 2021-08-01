/*
 * @(#)DrawStylesheets.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.draw;


public class DrawStylesheets {
    private DrawStylesheets() {
    }

    public static String getInspectorsStylesheet() {
        return DrawStylesheets.class.getResource("/org/jhotdraw8/draw/inspector/inspector.css").toString();
    }
}
