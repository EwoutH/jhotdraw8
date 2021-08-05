/*
 * @(#)TeddyApplication.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.samples.teddy;

import javafx.collections.ObservableMap;
import javafx.stage.Screen;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.app.AbstractFileBasedApplication;
import org.jhotdraw8.app.action.Action;
import org.jhotdraw8.fxml.FxmlUtil;
import org.jhotdraw8.gui.FileURIChooser;
import org.jhotdraw8.gui.URIExtensionFilter;

import java.util.Arrays;
import java.util.List;

import static org.jhotdraw8.app.action.file.AbstractOpenFileAction.OPEN_CHOOSER_FACTORY_KEY;
import static org.jhotdraw8.app.action.file.AbstractSaveFileAction.SAVE_CHOOSER_FACTORY_KEY;

/**
 * TeddyApplication.
 *
 * @author Werner Randelshofer
 */
public class TeddyApplication extends AbstractFileBasedApplication {

    @Override
    protected void initResourceBundle() {
        setResources(TeddyLabels.getResources());
    }

    @Override
    protected void initFactories() {
        setActivityFactory(FxmlUtil.createFxmlControllerSupplier(
                TeddyApplication.class.getResource("TeddyActivity.fxml"),
                TeddyLabels.getResources().asResourceBundle(),
                TeddyActivity::new));
        setMenuBarFactory(FxmlUtil.createFxmlNodeSupplier(
                TeddyApplication.class.getResource("TeddyMenuBar.fxml"),
                TeddyLabels.getResources().asResourceBundle()));
    }

    @Override
    protected void initProperties() {
        set(NAME_KEY, "Teddy");
        set(COPYRIGHT_KEY, "The authors and contributors of JHotDraw.");
        set(LICENSE_KEY, "MIT License");
        List<URIExtensionFilter> extensions = Arrays.asList(new URIExtensionFilter("Text Files", "text/plain", "*.txt"));
        set(SAVE_CHOOSER_FACTORY_KEY, () -> new FileURIChooser(FileURIChooser.Mode.SAVE, extensions));
        set(OPEN_CHOOSER_FACTORY_KEY, () -> new FileURIChooser(FileURIChooser.Mode.OPEN, extensions));
    }

    @Override
    protected void initActions(@NonNull ObservableMap<String, Action> map) {
        super.initActions(map);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (Screen.getPrimary().getOutputScaleX() >= 2.0) {
            // The following settings improve font rendering quality on
            // retina displays (no color fringes around characters).
            System.setProperty("prism.subpixeltext", "on");
            System.setProperty("prism.lcdtext", "false");
        } else {
            // The following settings improve font rendering on
            // low-res lcd displays (less color fringes around characters).
            System.setProperty("prism.text", "t2k");
            System.setProperty("prism.lcdtext", "true");
        }

        launch(args);
    }

}
