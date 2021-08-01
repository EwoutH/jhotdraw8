/*
 * @(#)FontTypeface.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.gui.fontchooser;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jhotdraw8.annotation.NonNull;

/**
 * FontTypeface.
 *
 * @author Werner Randelshofer
 */
public class FontTypeface {

    private final StringProperty name = new SimpleStringProperty();
    private final BooleanProperty regular = new SimpleBooleanProperty();
    private final StringProperty shortName = new SimpleStringProperty();

    public String getName() {
        return name.get();
    }

    public void setName(String value) {
        name.set(value);
    }

    public String getShortName() {
        return shortName.get();
    }

    public void setShortName(String value) {
        shortName.set(value);
    }

    public boolean isRegular() {
        return regular.get();
    }

    public void setRegular(boolean value) {
        regular.set(value);
    }

    public @NonNull StringProperty nameProperty() {
        return name;
    }

    public @NonNull BooleanProperty regularProperty() {
        return regular;
    }

    public @NonNull StringProperty shortNameProperty() {
        return shortName;
    }

    @Override
    public String toString() {
        return getShortName();
    }

}
