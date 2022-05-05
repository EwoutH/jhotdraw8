/*
 * @(#)FileBasedApplication.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.app;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.collection.key.NonNullKey;
import org.jhotdraw8.collection.key.SimpleNonNullKey;

public interface FileBasedApplication extends Application {
    @NonNull
    NonNullKey<Boolean> ALLOW_MULTIPLE_ACTIVITIES_WITH_SAME_URI = new SimpleNonNullKey<Boolean>("allowMultipleActivitiesWithSameURI", Boolean.class,
            Boolean.FALSE);
}
