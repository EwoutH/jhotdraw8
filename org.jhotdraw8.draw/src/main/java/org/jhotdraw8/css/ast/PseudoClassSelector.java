/*
 * @(#)PseudoClassSelector.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.css.ast;

/**
 * A "pseudo class selector" matches an element based on criteria which are not
 * directly encoded in the element.
 *
 * @author Werner Randelshofer
 */
public abstract class PseudoClassSelector extends SimpleSelector {

    @Override
    public final int getSpecificity() {
        return 10;
    }

}
