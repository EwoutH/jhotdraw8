/*
 * @(#)SimpleXmlWriter.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.draw.io;

import javafx.css.StyleOrigin;
import javafx.scene.input.DataFormat;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.base.converter.IdFactory;
import org.jhotdraw8.collection.facade.ReadOnlyMapFacade;
import org.jhotdraw8.collection.immutable.ImmutableList;
import org.jhotdraw8.collection.readonly.ReadOnlyMap;
import org.jhotdraw8.collection.typesafekey.CompositeMapAccessor;
import org.jhotdraw8.collection.typesafekey.Key;
import org.jhotdraw8.collection.typesafekey.MapAccessor;
import org.jhotdraw8.draw.figure.Clipping;
import org.jhotdraw8.draw.figure.ClippingFigure;
import org.jhotdraw8.draw.figure.Drawing;
import org.jhotdraw8.draw.figure.Figure;
import org.jhotdraw8.draw.input.ClipboardOutputFormat;
import org.jhotdraw8.fxbase.concurrent.WorkState;
import org.jhotdraw8.xml.IndentingXMLStreamWriter;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * SimpleXmlWriter.
 * <p>
 * Represents each Figure by an element, and each figure property by an
 * attribute.
 * <p>
 * All attribute values are treated as value types, except if an attribute type
 * is an instance of Figure.
 * <p>
 * This writer only works for drawings which can be described entirely by
 * the properties of its figures.
 * <p>
 * Attempts to preserve comments in the XML file, by associating
 * them to the figures and to the drawing.
 * <p>
 * Does not preserve whitespace in the XML file.
 *
 * @author Werner Randelshofer
 */
public class SimpleXmlWriter implements OutputFormat, ClipboardOutputFormat {
    protected FigureFactory figureFactory;
    protected IdFactory idFactory;
    protected String namespaceQualifier;
    protected String namespaceURI;
    private @NonNull ReadOnlyMap<Key<?>, Object> options = new ReadOnlyMapFacade<>(new LinkedHashMap<>());

    public SimpleXmlWriter(FigureFactory factory, IdFactory idFactory) {
        this(factory, idFactory, null, null);
    }

    public SimpleXmlWriter(FigureFactory factory, IdFactory idFactory, String namespaceURI, String namespaceQualifier) {
        this.figureFactory = factory;
        this.idFactory = idFactory;
        this.namespaceURI = namespaceURI;
        this.namespaceQualifier = namespaceQualifier;
    }

    private @NonNull DataFormat getDataFormat() {
        String mimeType = "application/xml";
        DataFormat df = DataFormat.lookupMimeType(mimeType);
        if (df == null) {
            df = new DataFormat(mimeType);
        }
        return df;
    }

    public boolean isNamespaceAware() {
        return namespaceURI != null;
    }

    public void setFigureFactory(FigureFactory figureFactory) {
        this.figureFactory = figureFactory;
    }

    public void setNamespaceURI(String namespaceURI) {
        this.namespaceURI = namespaceURI;
    }

    public Document toDocument(@Nullable URI documentHome, @NonNull Drawing internal, @NonNull Collection<Figure> selection) throws IOException {
        if (selection.isEmpty() || selection.contains(internal)) {
            return toDocument(documentHome, internal);
        }

        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            DocumentBuilder builder = null;
            builder = builderFactory.newDocumentBuilder();
            // We do not want that the builder creates a socket connection!
            builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            Document doc = builder.newDocument();
            DOMResult result = new DOMResult(doc);
            XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter w = xmlOutputFactory.createXMLStreamWriter(result);

            writeClipping(w, internal, selection, documentHome);

            w.close();
            return doc;
        } catch (ParserConfigurationException | XMLStreamException e) {
            throw new IOException("Could not create document builder.", e);
        }

    }

    public Document toDocument(@Nullable URI documentHome, @NonNull Drawing internal) throws IOException {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            // We do not want that the builder creates a socket connection!
            builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            Document doc = builder.newDocument();
            DOMResult result = new DOMResult(doc);
            XMLStreamWriter w = XMLOutputFactory.newInstance().createXMLStreamWriter(result);
            writeDocument(w, documentHome, internal);
            w.close();
            return doc;
        } catch (XMLStreamException | ParserConfigurationException e) {
            throw new IOException("Error writing to DOM.", e);
        }
    }

    @Override
    public void setOptions(@NonNull ReadOnlyMap<Key<?>, Object> newValue) {
        options = newValue;
    }

    @Override
    public @NonNull ReadOnlyMap<Key<?>, Object> getOptions() {
        return options;
    }

    @Override
    public void write(@NonNull OutputStream out, @Nullable URI documentHome, @NonNull Drawing drawing, @NonNull WorkState<Void> workState) throws IOException {
        write(documentHome, new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)),
                drawing, workState);
    }

    protected void write(@Nullable URI documentHome, @NonNull Writer out, @NonNull Drawing drawing, @NonNull WorkState<Void> workState) throws IOException {
        IndentingXMLStreamWriter w = new IndentingXMLStreamWriter(out);
        workState.updateProgress(0.0);
        try {
            writeDocument(w, documentHome, drawing);
            w.flush();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(@NonNull Map<DataFormat, Object> out, Drawing drawing, Collection<Figure> selection) throws IOException {
        StringWriter sw = new StringWriter();
        IndentingXMLStreamWriter w = new IndentingXMLStreamWriter(sw);
        URI documentHome = null;
        try {
            if (selection == null || selection.isEmpty()) {
                writeDocument(w, documentHome, drawing);
            } else {
                writeClipping(w, drawing, selection, documentHome);
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }

        out.put(getDataFormat(), sw.toString());
    }

    protected void writeClipping(@NonNull XMLStreamWriter w, @NonNull Drawing internal, @NonNull Collection<Figure> selection, @Nullable URI documentHome) throws IOException, XMLStreamException {
        // bring selection in z-order
        Set<Figure> s = new HashSet<>(selection);
        ArrayList<Figure> ordered = new ArrayList<>(selection.size());
        for (Figure f : internal.preorderIterable()) {
            if (s.contains(f)) {
                ordered.add(f);
            }
        }
        Clipping external = new ClippingFigure();
        idFactory.reset();
        idFactory.setDocumentHome(documentHome);
        final String docElemName = figureFactory.getElementNameByFigure(external);
        w.writeStartDocument();
        w.setDefaultNamespace(namespaceURI);
        w.writeStartElement(docElemName);
        w.writeDefaultNamespace(namespaceURI);
        for (Figure child : ordered) {
            writeNodeRecursively(w, child, 1);
        }
        w.writeEndElement();
        w.writeEndDocument();
    }

    protected void writeDocument(@NonNull XMLStreamWriter w, @Nullable URI documentHome, @NonNull Drawing internal) throws XMLStreamException {
        try {
            Drawing external = figureFactory.toExternalDrawing(internal);
            idFactory.reset();
            idFactory.setDocumentHome(documentHome);
            final String docElemName = figureFactory.getElementNameByFigure(external);
            w.writeStartDocument();
            w.setDefaultNamespace(namespaceURI);
            writeProcessingInstructions(w, external);
            w.writeStartElement(docElemName);
            w.writeDefaultNamespace(namespaceURI);
            writeElementAttributes(w, external);
            for (Figure child : external.getChildren()) {
                writeNodeRecursively(w, child, 1);
            }
            w.writeEndElement();
            w.writeEndDocument();
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    private void writeElementAttribute(@NonNull XMLStreamWriter w, @NonNull Figure figure, MapAccessor<Object> k) throws IOException, XMLStreamException {
        Object value = figure.get(k);
        if (!k.isTransient() && !figureFactory.isDefaultValue(figure, k, value)) {
            String name = figureFactory.getAttributeNameByKey(figure, k);
            if (figureFactory.getObjectIdAttribute().equals(name)) {
                return;
            }
            if (Figure.class.isAssignableFrom(k.getRawValueType())) {
                w.writeAttribute(name, idFactory.createId(value));
            } else {
                w.writeAttribute(name, figureFactory.valueToString(k, value));
            }
        }
    }

    protected void writeElementAttributes(@NonNull XMLStreamWriter w, @NonNull Figure figure) throws IOException, XMLStreamException {
        String id = idFactory.createId(figure);
        String objectIdAttribute = figureFactory.getObjectIdAttribute();
        w.writeAttribute(objectIdAttribute, id);
        final Set<MapAccessor<?>> keys = figureFactory.figureAttributeKeys(figure);
        Set<MapAccessor<?>> done = new HashSet<>(keys.size());

        // First write all non-transient composite attributes, then write the remaining non-transient non-composite attributes
        for (MapAccessor<?> k : keys) {
            if (k instanceof CompositeMapAccessor) {
                done.add(k);
                if (!k.isTransient()) {
                    @SuppressWarnings("unchecked") CompositeMapAccessor<Object> cmap = (CompositeMapAccessor<Object>) k;
                    done.addAll(cmap.getSubAccessors());
                    writeElementAttribute(w, figure, cmap);
                }
            }
        }
        for (MapAccessor<?> k : keys) {
            if (!k.isTransient() && !done.contains(k)) {
                @SuppressWarnings("unchecked") MapAccessor<Object> cmap = (MapAccessor<Object>) k;
                writeElementAttribute(w, figure, cmap);
            }
        }
    }

    private void writeElementNodeList(@NonNull XMLStreamWriter w, @NonNull Figure figure) throws IOException, XMLStreamException {
        for (MapAccessor<?> k : figureFactory.figureNodeListKeys(figure)) {
            @SuppressWarnings("unchecked")
            MapAccessor<Object> key = (MapAccessor<Object>) k;
            Object value = figure.get(key);
            if (!key.isTransient() && figure.containsMapAccessor(StyleOrigin.USER, key) && !figureFactory.isDefaultValue(figure, key, value)) {
                figureFactory.valueToNodeList(key, value, w);
            }
        }
    }

    protected void writeNodeRecursively(@NonNull XMLStreamWriter w, @NonNull Figure figure, int depth) throws IOException {
        try {
            String elementName = figureFactory.getElementNameByFigure(figure);
            if (elementName == null) {
                // => the figureFactory decided that we should skip the figure
                return;
            }
            w.writeStartElement(elementName);
            writeElementAttributes(w, figure);
            writeElementNodeList(w, figure);
            for (Figure child : figure.getChildren()) {
                if (figureFactory.getElementNameByFigure(child) != null) {
                    writeNodeRecursively(w, child, depth + 1);
                }
            }
            w.writeEndElement();
        } catch (IOException | XMLStreamException e) {
            throw new IOException("Error writing figure " + figure, e);
        }
    }

    // XXX maybe this should not be in SimpleXmlIO?
    protected void writeProcessingInstructions(@NonNull XMLStreamWriter w, @NonNull Drawing external) throws XMLStreamException {
        if (figureFactory.getStylesheetsKey() != null) {
            ImmutableList<URI> stylesheets = external.get(figureFactory.getStylesheetsKey());
            if (stylesheets != null) {
                for (URI stylesheet : stylesheets) {
                    stylesheet = idFactory.relativize(stylesheet);

                    String stylesheetString = stylesheet.toString();
                    String type = "text/" + stylesheetString.substring(stylesheetString.lastIndexOf('.') + 1);
                    if ("text/".equals(type)) {
                        type = "text/css";
                    }
                    w.writeProcessingInstruction("xml-stylesheet", //
                            "type=\"" + type + "\" href=\"" + stylesheet + "\"");
                }
            }
        }
    }
}
