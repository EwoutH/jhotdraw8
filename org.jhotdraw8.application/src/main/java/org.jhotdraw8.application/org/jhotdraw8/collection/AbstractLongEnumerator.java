/*
 * @(#)AbstractEnumeratorSpliterator.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.NonNull;

import java.util.Spliterators;

/**
 * AbstractLongEnumerator.
 *
 * @author Werner Randelshofer
 */
public abstract class AbstractLongEnumerator extends Spliterators.AbstractLongSpliterator
        implements LongEnumerator {
    protected long current;

    /**
     * Creates a spliterator reporting the given estimated size and
     * additionalCharacteristics.
     *
     * @param est                       the estimated size of this spliterator if known, otherwise
     *                                  {@code Long.MAX_VALUE}.
     * @param additionalCharacteristics properties of this spliterator's
     *                                  source or elements.  If {@code SIZED} is reported then this
     *                                  spliterator will additionally report {@code SUBSIZED}.
     */
    protected AbstractLongEnumerator(long est, int additionalCharacteristics) {
        super(est, additionalCharacteristics);
    }

    @Override
    public final long currentAsLong() {
        return current;
    }

    @Override
    public final @NonNull Long current() {
        return current;
    }
}
