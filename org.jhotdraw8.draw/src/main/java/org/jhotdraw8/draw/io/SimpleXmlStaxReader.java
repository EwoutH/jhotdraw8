/*
 * @(#)SimpleXmlStaxReader.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.draw.io;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.collection.ImmutableList;
import org.jhotdraw8.collection.ImmutableLists;
import org.jhotdraw8.collection.Key;
import org.jhotdraw8.collection.MapAccessor;
import org.jhotdraw8.concurrent.SimpleWorkState;
import org.jhotdraw8.concurrent.WorkState;
import org.jhotdraw8.draw.figure.Drawing;
import org.jhotdraw8.draw.figure.Figure;
import org.jhotdraw8.draw.figure.Layer;
import org.jhotdraw8.draw.figure.StyleableFigure;
import org.jhotdraw8.draw.input.ClipboardInputFormat;
import org.jhotdraw8.draw.model.DrawingModel;
import org.jhotdraw8.io.IdFactory;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This reader does not support {@link FigureFactory#nodeListToValue(MapAccessor, List)}.
 */
public class SimpleXmlStaxReader implements InputFormat, ClipboardInputFormat {
    private static final Pattern hrefPattern = Pattern.compile("(?:^|.* )href=\"([^\"]*)\".*");
    private final @NonNull ObservableMap<Key<?>, Object> properties = FXCollections.observableHashMap();
    private final @NonNull IdFactory idFactory;
    private @Nullable String namespaceURI;
    private @NonNull FigureFactory figureFactory;
    private String idAttribute = "id";
    private Supplier<Layer> layerFactory;

    public SimpleXmlStaxReader(@NonNull FigureFactory figureFactory, @NonNull IdFactory idFactory, @Nullable String namespaceURI) {
        this.idFactory = idFactory;
        this.figureFactory = figureFactory;
        this.namespaceURI = namespaceURI;
    }

    private @NonNull Figure createFigure(@NonNull XMLStreamReader r, @NonNull Deque<Figure> stack) throws IOException {
        Figure figure;
        try {
            figure = figureFactory.createFigureByElementName(r.getLocalName());
        } catch (IOException e) {
            throw new IOException("Cannot create figure for element <" + r.getLocalName() + "> at line " + r.getLocation().getLineNumber() + ", col " + r.getLocation().getColumnNumber(), e);
        }
        if (!stack.isEmpty()) {
            Figure parent = stack.peek();
            if (!figure.isSuitableParent(parent) || !parent.isSuitableChild(figure)) {
                throw new IOException("Cannot add figure to parent in element <" + r.getLocalName() + "> at line " + r.getLocation().getLineNumber() + ", col " + r.getLocation().getColumnNumber());
            }
            parent.getChildren().add(figure);
        } else {
            stack.addFirst(figure);// add twice, so that it will remain after we finish the file
        }
        stack.addFirst(figure);
        return figure;
    }

    private DataFormat getDataFormat() {
        String mimeType = "application/xml";
        DataFormat df = DataFormat.lookupMimeType(mimeType);
        if (df == null) {
            df = new DataFormat(mimeType);
        }
        return df;
    }

    public @NonNull IdFactory getIdFactory() {
        return idFactory;
    }

    public Supplier<Layer> getLayerFactory() {
        return layerFactory;
    }

    public void setLayerFactory(Supplier<Layer> layerFactory) {
        this.layerFactory = layerFactory;
    }

    @Override
    public @NonNull ObservableMap<Key<?>, Object> getProperties() {
        return properties;
    }

    @Override
    public @Nullable Figure read(@NonNull InputStream in, Drawing drawing, URI documentHome, @NonNull WorkState workState) throws IOException {
        idFactory.setDocumentHome(documentHome);
        XMLInputFactory dbf = XMLInputFactory.newInstance();

        // We do not want that the reader creates a socket connection,
        // even if it would benefit the result!
        dbf.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        dbf.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        dbf.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        dbf.setXMLResolver((publicID,
                            systemID,
                            baseURI,
                            namespace) -> null
        );
        Deque<Figure> stack = new ArrayDeque<>();
        List<Runnable> secondPass = new ArrayList<>();
        try {
            for (XMLStreamReader r = dbf.createXMLStreamReader(in); r.hasNext(); ) {
                readNode(r, r.next(), stack, secondPass);
            }

        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
        if (stack.size() > 1) {
            throw new IOException("Illegal stack size! " + stack);
        }

        try {
            secondPass.parallelStream().forEach(Runnable::run);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } catch (Exception e) {
            throw new IOException(e);
        }

        Figure figure = stack.isEmpty() ? null : stack.getFirst();
        if ((figure instanceof Drawing)) {
            figure.set(Drawing.DOCUMENT_HOME, documentHome);
        }
        return figure;
    }

    public @Nullable Figure read(@NonNull Reader in, Drawing drawing, URI documentHome, @NonNull WorkState workState) throws IOException {
        idFactory.setDocumentHome(documentHome);
        XMLInputFactory dbf = XMLInputFactory.newInstance();

        // We do not want that the reader creates a socket connection,
        // even if it would benefit the result!
        dbf.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        dbf.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        dbf.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        dbf.setXMLResolver((publicID,
                            systemID,
                            baseURI,
                            namespace) -> null
        );
        Deque<Figure> stack = new ArrayDeque<>();
        List<Runnable> secondPass = new ArrayList<>();
        try {
            for (XMLStreamReader r = dbf.createXMLStreamReader(in); r.hasNext(); ) {
                readNode(r, r.next(), stack, secondPass);
            }

        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
        if (stack.size() > 1) {
            throw new IOException("Illegal stack size! " + stack);
        }

        try {
            secondPass.parallelStream().forEach(Runnable::run);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } catch (Exception e) {
            throw new IOException(e);
        }

        Figure figure = stack.isEmpty() ? null : stack.getFirst();
        if ((figure instanceof Drawing)) {
            figure.set(Drawing.DOCUMENT_HOME, documentHome);
        }
        return figure;
    }

    @Override
    public Set<Figure> read(Clipboard clipboard, DrawingModel model, Drawing drawing, @Nullable Figure parent) throws IOException {
        Object content = clipboard.getContent(getDataFormat());
        if (content instanceof String) {
            Set<Figure> figures = new LinkedHashSet<>();
            Figure newDrawing = read(new StringReader((String) content), null, null, new SimpleWorkState());
            idFactory.reset();
            idFactory.setDocumentHome(null);
            for (Figure f : drawing.preorderIterable()) {
                idFactory.createId(f);
            }
            // FIXME use current layer in drawingView!
            Layer layer = layerFactory.get();
            for (Figure f : new ArrayList<>(newDrawing.getChildren())) {
                figures.add(f);
                newDrawing.removeChild(f);
                String id = idFactory.createId(f);
                f.set(StyleableFigure.ID, id);
                if (f instanceof Layer) {
                    model.addChildTo(f, drawing);
                } else {
                    if (layer.getParent() == null) {
                        model.addChildTo(layer, drawing);
                    }
                    model.addChildTo(f, layer);
                }
            }
            return figures;
        } else {
            throw new IOException("no data found");
        }

    }

    private void readAttributes(@NonNull XMLStreamReader r, @NonNull Figure figure, @NonNull List<Runnable> secondPass) throws IOException {
        for (int i = 0, n = r.getAttributeCount(); i < n; i++) {
            String ns = r.getAttributeNamespace(i);
            if (namespaceURI != null && ns != null && !namespaceURI.equals(ns)) {
                continue;
            }
            String attributeLocalName = r.getAttributeLocalName(i);
            String attributeValue = r.getAttributeValue(i);
            Location location = r.getLocation();
            if (idAttribute.equals(attributeLocalName)) {
                idFactory.putIdToObject(attributeValue, figure);
                /*
                Object anotherObjWithSameId = idFactory.putIdToObject(attributeValue, figure);
                if (anotherObjWithSameId != null) {
                    // Note: it is okay if we have found a duplicate id when pasting!
                    throw new IOException("Duplicate id " + attributeValue + " at line " + location.getLineNumber() + ", col " + location.getColumnNumber());
                }*/
                setId(figure, attributeValue);
            } else {
                @SuppressWarnings("unchecked")
                MapAccessor<Object> key =
                        (MapAccessor<Object>) figureFactory.getKeyByAttributeName(figure, attributeLocalName);
                if (key == null) {
                    throw new IOException("Unsupported attribute \"" + attributeLocalName + "\" at line " + location.getLineNumber() + ", col " + location.getColumnNumber());
                }
                if (figureFactory.needsIdResolver(key)) {
                    secondPass.add(() -> {
                        try {
                            figure.set(key, figureFactory.stringToValue(key, attributeValue));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                } else {
                    figure.set(key, figureFactory.stringToValue(key, attributeValue));
                }
            }
        }
    }

    private void readEndElement(@NonNull XMLStreamReader r, @NonNull Deque<Figure> stack) {
        stack.removeFirst();
    }

    private void readNode(XMLStreamReader r, int next, @NonNull Deque<Figure> stack, @NonNull List<Runnable> secondPass) throws IOException {
        switch (next) {
        case XMLStreamReader.START_ELEMENT:
            readStartElement(r, stack, secondPass);
            break;
        case XMLStreamReader.END_ELEMENT:
            readEndElement(r, stack);
            break;
        case XMLStreamReader.PROCESSING_INSTRUCTION:
            readProcessingInstruction(r, stack, secondPass);
            break;
        case XMLStreamReader.CHARACTERS:
        case XMLStreamReader.ENTITY_DECLARATION:
        case XMLStreamReader.NOTATION_DECLARATION:
        case XMLStreamReader.NAMESPACE:
        case XMLStreamReader.CDATA:
        case XMLStreamReader.DTD:
        case XMLStreamReader.ATTRIBUTE:
        case XMLStreamReader.ENTITY_REFERENCE:
        case XMLStreamReader.END_DOCUMENT:
        case XMLStreamReader.START_DOCUMENT:
        case XMLStreamReader.SPACE:
        case XMLStreamReader.COMMENT:
            break;
        default:
            throw new IOException("unsupported XMLStream event: " + next);
        }
    }

    private void readProcessingInstruction(XMLStreamReader r, @NonNull Deque<Figure> stack, List<Runnable> secondPass) {
        if (figureFactory.getStylesheetsKey() != null) {
            String piTarget = r.getPITarget();
            String piData = r.getPIData();

            if ("xml-stylesheet".equals(piTarget) && piData != null) {
                secondPass.add(() -> {
                    Matcher m = hrefPattern.matcher(piData);
                    if (m.matches()) {
                        String href = m.group(1);

                        URI uri = URI.create(href);
                        uri = idFactory.absolutize(uri);
                        Figure drawing = stack.getFirst();
                        synchronized (drawing) {
                            ImmutableList<URI> listOrNull = drawing.get(figureFactory.getStylesheetsKey());
                            List<URI> stylesheets = listOrNull == null ? new ArrayList<>() : new ArrayList<>(listOrNull.asList());
                            stylesheets.add(uri);
                            drawing.set(figureFactory.getStylesheetsKey(), ImmutableLists.ofCollection(stylesheets));
                            ImmutableList<URI> listOrNull2 = drawing.get(figureFactory.getStylesheetsKey());
                        }
                    }
                });

            }
        }
    }

    private void readStartElement(@NonNull XMLStreamReader r, @NonNull Deque<Figure> stack, @NonNull List<Runnable> secondPass) throws IOException {
        if (namespaceURI != null && !namespaceURI.equals(r.getNamespaceURI())) {
            return;
        }

        Figure figure = createFigure(r, stack);
        readAttributes(r, figure, secondPass);
    }

    public void setFigureFactory(@NonNull FigureFactory figureFactory) {
        this.figureFactory = figureFactory;
    }

    protected void setId(@NonNull Figure figure, String id) {
        figure.set(StyleableFigure.ID, id);
    }

    public void setNamespaceURI(@Nullable String namespaceURI) {
        this.namespaceURI = namespaceURI;
    }
}
