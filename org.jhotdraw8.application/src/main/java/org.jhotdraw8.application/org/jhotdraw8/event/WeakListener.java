/*
 * @(#)WeakListener.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.event;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.EventObject;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * WeakListener.
 *
 * @author Werner Randelshofer
 */
public final class WeakListener<E extends EventObject> implements Listener<E>, javafx.beans.WeakListener {

    private final @NonNull WeakReference<Listener<E>> ref;
    private Consumer<Listener<E>> removeListener;

    public WeakListener(@Nullable Listener<E> listener, Consumer<Listener<E>> removeListener) {
        Objects.requireNonNull(listener, "listener is null");
        this.ref = new WeakReference<>(listener);
        this.removeListener = removeListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean wasGarbageCollected() {
        return (ref.get() == null);
    }

    @Override
    public void handle(E event) {
        Listener<E> listener = ref.get();
        if (listener != null) {
            listener.handle(event);
        } else {
            // The weakly reference listener has been garbage collected,
            // so this WeakListener will now unhook itself from the
            // source bean
            removeListener.accept(this);
        }
    }

}
