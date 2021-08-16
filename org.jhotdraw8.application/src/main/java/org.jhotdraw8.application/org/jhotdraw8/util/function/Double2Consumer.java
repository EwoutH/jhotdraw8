/*
 * @(#)Double2Consumer.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.util.function;

/**
 * Double2Consumer.
 *
 * @author Werner Randelshofer
 */
@FunctionalInterface
public interface Double2Consumer {

    /**
     * Performs this operation on the given argument.
     *
     * @param v1 the input argument
     * @param v2 the input argument
     */
    void accept(double v1, double v2);

}
