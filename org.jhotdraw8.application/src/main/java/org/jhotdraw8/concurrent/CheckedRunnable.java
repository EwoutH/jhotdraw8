/*
 * @(#)CheckedRunnable.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.concurrent;

/**
 * A Runnable that may throw a checked exception.
 *
 * @author Werner Randelshofer
 */
public interface CheckedRunnable {

    void run() throws Exception;
}
