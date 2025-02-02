/*
 * @(#)ImageNodeReader.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.fxbase.spi;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.jhotdraw8.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ImageNodeReader implements NodeReader {
    public ImageNodeReader() {
    }

    @Override
    public Node read(@NonNull URL url) throws IOException {
        return new ImageView(url.toString());
    }

    @Override
    public Node read(@NonNull InputStream in) throws IOException {
        return new ImageView(new Image(in));
    }
}
