/*
 * @(#)CheckedRunnable.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.base.concurrent;

/**
 * A runnable that may throw a checked exception.
 *
 * @author Werner Randelshofer
 */
@FunctionalInterface
public interface CheckedRunnable {

    void run() throws Exception;
}
