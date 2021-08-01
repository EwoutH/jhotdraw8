/*
 * @(#)FontFamily.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.gui.fontchooser;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jhotdraw8.annotation.NonNull;

/**
 * FontFamily.
 *
 * @author Werner Randelshofer
 */
public class FontFamily {
    private final StringProperty name = new SimpleStringProperty();

    private final ObservableList<FontTypeface> typefaces = FXCollections.observableArrayList();

    public @NonNull ObservableList<FontTypeface> getTypefaces() {
        return typefaces;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String value) {
        name.set(value);
    }

    public @NonNull StringProperty nameProperty() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

}
