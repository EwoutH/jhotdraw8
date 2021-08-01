/*
 * @(#)SimpleStylesheetsManager.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.css;

import javafx.css.StyleOrigin;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.collection.ImmutableList;
import org.jhotdraw8.collection.ReadOnlyList;
import org.jhotdraw8.css.ast.Declaration;
import org.jhotdraw8.css.ast.Selector;
import org.jhotdraw8.css.ast.StyleRule;
import org.jhotdraw8.css.ast.Stylesheet;
import org.jhotdraw8.css.function.CssFunction;
import org.jhotdraw8.io.SimpleUriResolver;
import org.jhotdraw8.io.UriResolver;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * SimpleStylesheetsManager.
 *
 * @param <E> the element type that can be styled by this style manager
 * @author Werner Randelshofer
 */
public class SimpleStylesheetsManager<E> implements StylesheetsManager<E> {

    private @NonNull Supplier<CssParser> parserFactory = CssParser::new;
    private @NonNull UriResolver uriResolver = new SimpleUriResolver();
    private @NonNull SelectorModel<E> selectorModel;
    /**
     * Cache for parsed user agent stylesheets.
     * <p>
     * The key is either an URI or a literal CSS String for which we cache the
     * data. The value contains the parsed stylesheet entry.
     */
    private @NonNull LinkedHashMap<Object, StylesheetEntry> userAgentList = new LinkedHashMap<>();
    /**
     * @see #userAgentList
     */
    private @NonNull LinkedHashMap<Object, StylesheetEntry> authorList = new LinkedHashMap<>();
    /**
     * @see #userAgentList
     */
    private @NonNull LinkedHashMap<Object, StylesheetEntry> inlineList = new LinkedHashMap<>();
    private final @NonNull Executor executor = Executors.newCachedThreadPool();
    private @Nullable Map<String, ImmutableList<CssToken>> cachedAuthorCustomProperties;
    private @Nullable Map<String, ImmutableList<CssToken>> cachedInlineCustomProperties;
    private @Nullable Map<String, ImmutableList<CssToken>> cachedUserAgentCustomProperties;
    private static final @NonNull Logger LOGGER = Logger.getLogger(SimpleStylesheetsManager.class.getName());

    public SimpleStylesheetsManager(@NonNull SelectorModel<E> selectorModel) {
        this(selectorModel, Collections.emptyList());
    }

    public SimpleStylesheetsManager(@NonNull SelectorModel<E> selectorModel, @NonNull List<CssFunction<E>> functions) {
        this.selectorModel = selectorModel;
        this.functions = functions;
    }

    private void doSetAttribute(@NonNull SelectorModel<E> selectorModel1, @NonNull E elem, @NonNull StyleOrigin styleOrigin,
                                @Nullable String namespace, @NonNull String name, @Nullable ImmutableList<CssToken> value,
                                Map<String, ImmutableList<CssToken>> customProperties,
                                @Nullable CssFunctionProcessor<E> functionProcessor) throws ParseException {
        if (value == null) {
            selectorModel1.setAttribute(elem, styleOrigin, namespace, name, null);
        } else {
            if (functionProcessor != null) {
                ImmutableList<CssToken> processed = preprocessTerms(elem, functionProcessor, value);
                selectorModel1.setAttribute(elem, styleOrigin, namespace, name, processed);
            } else {
                selectorModel1.setAttribute(elem, styleOrigin, namespace, name, value);
            }
        }
    }

    public void setSelectorModel(@NonNull SelectorModel<E> newValue) {
        selectorModel = newValue;
    }

    @Override
    public @NonNull SelectorModel<E> getSelectorModel() {
        return selectorModel;
    }

    @Override
    public void addStylesheet(@NonNull StyleOrigin origin, @Nullable URI documentHome, @NonNull URI uri) {
        URI absolutizedUri = uriResolver.absolutize(documentHome, uri);
        invalidate();
        getMap(origin).put(absolutizedUri, new StylesheetEntry(origin, absolutizedUri));
    }

    @Override
    public void addStylesheet(@NonNull StyleOrigin origin, @NonNull Stylesheet stylesheet) {
        invalidate();
        getMap(origin).put(stylesheet, new StylesheetEntry(origin, stylesheet));
    }

    @Override
    public void addStylesheet(@NonNull StyleOrigin origin, @NonNull String str, @Nullable URI documentHome) {
        invalidate();
        getMap(origin).put(str, new StylesheetEntry(origin, str, documentHome));
    }

    private void invalidate() {
        cachedAuthorCustomProperties = null;
        cachedInlineCustomProperties = null;
        cachedUserAgentCustomProperties = null;
    }

    @Override
    public void clearStylesheets(@Nullable StyleOrigin origin) {
        if (origin == null) {
            authorList.clear();
            userAgentList.clear();
            inlineList.clear();
            invalidate();
        } else {
            getMap(origin).clear();
        }
    }

    private LinkedHashMap<Object, StylesheetEntry> getMap(@NonNull StyleOrigin origin) {
        switch (origin) {
        case AUTHOR:
            return authorList;
        case USER_AGENT:
            return userAgentList;
        case INLINE:
            return inlineList;
        default:
            throw new IllegalArgumentException("illegal origin:" + origin);
        }
    }

    private void setMap(@NonNull StyleOrigin origin, LinkedHashMap<Object, StylesheetEntry> newValue) {
        switch (origin) {
        case AUTHOR:
            authorList = newValue;
            break;
        case USER_AGENT:
            userAgentList = newValue;
            break;
        case INLINE:
            inlineList = newValue;
            break;
        default:
            throw new IllegalArgumentException("illegal origin:" + origin);
        }
    }

    @Override
    public <T> void setStylesheets(@NonNull StyleOrigin origin, @Nullable URI documentHome, @Nullable List<T> stylesheets) {
        invalidate();
        LinkedHashMap<Object, StylesheetEntry> oldMap = getMap(origin);
        if (stylesheets == null) {
            oldMap.clear();
            return;
        }
        LinkedHashMap<Object, StylesheetEntry> newMap = new LinkedHashMap<>();
        for (T t : stylesheets) {
            if (t instanceof URI) {
                URI uri = (URI) t;
                URI absolutizedUri = uriResolver.absolutize(documentHome, uri);
                StylesheetEntry old = oldMap.get(absolutizedUri);
                newMap.put(absolutizedUri, new StylesheetEntry(origin, absolutizedUri));
            } else if (t instanceof String) {
                StylesheetEntry old = oldMap.get(t);
                if (old != null) {
                    newMap.put(t, old);
                } else {
                    newMap.put(t, new StylesheetEntry(origin, (String) t, documentHome));
                }
            } else {
                throw new IllegalArgumentException("illegal item " + t);
            }
        }
        setMap(origin, newMap);
    }

    protected @NonNull Collection<StylesheetEntry> getAuthorStylesheets() {
        return authorList.values();
    }

    protected @NonNull Collection<StylesheetEntry> getUserAgentStylesheets() {
        return userAgentList.values();
    }

    protected @NonNull Collection<StylesheetEntry> getInlineStylesheets() {
        return inlineList.values();
    }

    @Override
    public void applyStylesheetsTo(@NonNull E elem) {
        applyStylesheetsTo(Collections.singleton(elem));
    }

    @Override
    public void applyStylesheetsTo(@NonNull Iterable<E> iterable) {
        SelectorModel<E> selectorModel = getSelectorModel();

        // Compute custom properties
        Map<String, ImmutableList<CssToken>> customProperties = computeCustomProperties();
        final CssFunctionProcessor<E> functionProcessor = functions.isEmpty() ? null : createCssFunctionProcessor(selectorModel, customProperties);

        StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList())
                .stream()
                .parallel()
                .forEach(elem -> {
                    // Clear stylesheet values
                    selectorModel.reset(elem);

                    // The stylesheet is a user-agent stylesheet
                    for (ApplicableDeclaration entry : collectApplicableDeclarations(elem, getUserAgentStylesheets())) {
                        try {
                            Declaration d = entry.getDeclaration();
                            doSetAttribute(selectorModel, elem, StyleOrigin.USER_AGENT, d.getNamespace(), d.getPropertyName(), d.getTerms(), customProperties, functionProcessor);
                        } catch (ParseException e) {
                            LOGGER.throwing(SimpleStylesheetsManager.class.getName(), "applyStylesheetsTo", e);
                        }
                    }

                    // The value of a property was set by the user through a call to a set method with StyleOrigin.USER
                    // ... nothing to do!

                    // The stylesheet is an external file
                    for (ApplicableDeclaration entry : collectApplicableDeclarations(elem, getAuthorStylesheets())) {
                        try {
                            Declaration d = entry.getDeclaration();
                            doSetAttribute(selectorModel, elem, StyleOrigin.AUTHOR, d.getNamespace(), d.getPropertyName(), d.getTerms(), customProperties, functionProcessor);
                        } catch (ParseException e) {
                            LOGGER.throwing(SimpleStylesheetsManager.class.getName(), "applyStylesheetsTo", e);
                        }
                    }

                    // The stylesheet is an internal file
                    for (ApplicableDeclaration entry : collectApplicableDeclarations(elem, getInlineStylesheets())) {
                        try {
                            Declaration d = entry.getDeclaration();
                            doSetAttribute(selectorModel, elem, StyleOrigin.INLINE, d.getNamespace(), d.getPropertyName(), d.getTerms(), customProperties, functionProcessor);
                        } catch (ParseException e) {
                            LOGGER.throwing(SimpleStylesheetsManager.class.getName(), "applyStylesheetsTo", e);
                        }
                    }

                    // 'inline style attributes' can override all other values
                    CssParser parser = parserFactory.get();
                    if (selectorModel.hasAttribute(elem, null, "style")) {
                        Map<QualifiedName, ImmutableList<CssToken>> inlineDeclarations = new HashMap<>();
                        String styleValue = selectorModel.getAttributeAsString(elem, null, "style");
                        if (styleValue != null) {
                            try {
                                for (Declaration d : parser.parseDeclarationList(styleValue)) {
                                    // Declarations without terms are ignored
                                    if (d.getTerms().isEmpty()) {
                                        continue;
                                    }

                                    inlineDeclarations.put(new QualifiedName(d.getNamespace(), d.getPropertyName()), d.getTerms());
                                }
                            } catch (IOException ex) {
                                System.err.println("DOMStyleManager: Invalid style attribute on element. style=" + styleValue);
                                ex.printStackTrace();
                            }
                        }
                        Map<String, ImmutableList<CssToken>> inlineStyleAttrCustomProperties = Collections.emptyMap();
                        for (Map.Entry<QualifiedName, ImmutableList<CssToken>> entry : inlineDeclarations.entrySet()) {
                            try {
                                doSetAttribute(selectorModel, elem, StyleOrigin.INLINE, entry.getKey().getNamespace(), entry.getKey().getName(), entry.getValue(), inlineStyleAttrCustomProperties, functionProcessor);
                            } catch (ParseException e) {
                                LOGGER.throwing(SimpleStylesheetsManager.class.getName(), "applyStylesheetsTo", e);
                            }
                        }
                        inlineDeclarations.clear();
                    }
                });
    }

    private @NonNull Map<String, ImmutableList<CssToken>> computeCustomProperties() {
        Map<String, ImmutableList<CssToken>> customProperties = new LinkedHashMap<>();
        customProperties.putAll(getUserAgentCustomProperties());
        customProperties.putAll(getAuthorCustomProperties());
        customProperties.putAll(getInlineCustomProperties());
        return customProperties;
    }

    private @NonNull Map<String, ImmutableList<CssToken>> getInlineCustomProperties() {
        if (cachedInlineCustomProperties == null) {
            cachedInlineCustomProperties = collectCustomProperties(getInlineStylesheets());
        }
        return cachedInlineCustomProperties;
    }

    private @NonNull Map<String, ImmutableList<CssToken>> getAuthorCustomProperties() {
        if (cachedAuthorCustomProperties == null) {
            cachedAuthorCustomProperties = collectCustomProperties(getAuthorStylesheets());
        }
        return cachedAuthorCustomProperties;
    }

    private @NonNull Map<String, ImmutableList<CssToken>> getUserAgentCustomProperties() {
        if (cachedUserAgentCustomProperties == null) {
            cachedUserAgentCustomProperties = collectCustomProperties(getUserAgentStylesheets());
        }
        return cachedUserAgentCustomProperties;
    }

    /**
     * Collects all declarations in all specified stylesheets which are
     * applicable to the specified element.
     *
     * @param elem        an element
     * @param stylesheets the stylesheets
     * @return list of applicable declarations
     */
    private List<ApplicableDeclaration> collectApplicableDeclarations(
            E elem,
            @NonNull Collection<StylesheetEntry> stylesheets) {
        List<ApplicableDeclaration> applicableDeclarations = new ArrayList<>();
        for (StylesheetEntry e : stylesheets) {
            Stylesheet s = e.getStylesheet();
            if (s == null) {
                continue;
            }
            collectApplicableDeclarations(elem, s, applicableDeclarations);
        }

        applicableDeclarations.sort(Comparator.comparingInt(ApplicableDeclaration::getSpecificity));
        return applicableDeclarations;
    }

    private static class ApplicableDeclaration {
        private final int specificity;
        private final Stylesheet stylesheet;
        private final Declaration declaration;

        public ApplicableDeclaration(int specificity, Stylesheet stylesheet, Declaration declaration) {
            this.specificity = specificity;
            this.stylesheet = stylesheet;
            this.declaration = declaration;
        }

        public Declaration getDeclaration() {
            return declaration;
        }

        public int getSpecificity() {
            return specificity;
        }
    }

    private @NonNull List<ApplicableDeclaration> collectApplicableDeclarations(
            E elem, @NonNull Stylesheet s,
            @NonNull List<ApplicableDeclaration> applicableDeclarations) {
        SelectorModel<E> selectorModel = getSelectorModel();
        for (StyleRule r : s.getStyleRules()) {
            Selector selector;
            if (null != (selector = r.getSelectorGroup().matchSelector(selectorModel, elem))) {
                for (Declaration d : r.getDeclarations()) {
                    // Declarations without terms are ignored
                    if (d.getTerms().isEmpty()) {
                        continue;
                    }

                    applicableDeclarations.add(new ApplicableDeclaration(selector.getSpecificity(),
                            s, d));
                }
            }
        }
        return applicableDeclarations;
    }

    @Override
    public boolean applyStylesheetTo(@NonNull StyleOrigin styleOrigin, @NonNull Stylesheet s, @NonNull E elem, boolean suppressParseException) throws ParseException {
        SelectorModel<E> selectorModel = getSelectorModel();
        final Map<String, ImmutableList<CssToken>> customProperties = collectCustomProperties(s);

        CssFunctionProcessor<E> processor = createCssFunctionProcessor(selectorModel, customProperties);
        final List<ApplicableDeclaration> applicableDeclarations = collectApplicableDeclarations(elem, s,
                new ArrayList<>());
        if (applicableDeclarations.isEmpty()) {
            return false;
        }
        for (ApplicableDeclaration entry : applicableDeclarations) {
            Declaration d = entry.getDeclaration();
            ImmutableList<CssToken> value = preprocessTerms(elem, processor, d.getTerms());
            try {

                ReadOnlyList<CssToken> appliedValue;
                CssToken first = value.size() == 0 ? null : value.getFirst();
                if (first != null && first.getType() == CssTokenType.TT_IDENT) {
                    switch (first.getStringValueNonNull()) {
                    case CssTokenType.IDENT_UNSET:
                        appliedValue = null;
                        break;
                    default:
                        appliedValue = value;
                        break;
                    }
                } else {
                    appliedValue = value;
                }
                selectorModel.setAttribute(elem, styleOrigin, d.getNamespace(), d.getPropertyName(),
                        appliedValue);
            } catch (ParseException e) {
                if (suppressParseException) {
                    LOGGER.throwing(SimpleStylesheetsManager.class.getName(), "applyStylesheetsTo", e);
                } else {
                    throw e;
                }
            }
        }
        return true;
    }

    private CssFunctionProcessor<E> createCssFunctionProcessor(SelectorModel<E> selectorModel, Map<String, ImmutableList<CssToken>> customProperties) {
        return new SimpleCssFunctionProcessor<>(functions, selectorModel, customProperties);
    }


    private @NonNull List<CssFunction<E>> functions = new ArrayList<>();

    public @NonNull List<CssFunction<E>> getFunctions() {
        return functions;
    }

    public void setFunctions(@NonNull List<CssFunction<E>> functions) {
        this.functions = functions;
    }

    @Override
    public @NonNull String getHelpText() {
        StringBuilder buf = new StringBuilder();
        for (CssFunction<E> value : functions) {
            if (buf.length() != 0) {
                buf.append("\n");
            }
            buf.append(value.getHelpText());
        }
        return buf.toString();
    }

    private @NonNull Map<String, ImmutableList<CssToken>> collectCustomProperties(@NonNull Collection<StylesheetEntry> stylesheets) {
        Map<String, ImmutableList<CssToken>> customProperties = new LinkedHashMap<>();
        for (StylesheetEntry s : stylesheets) {
            Stylesheet stylesheet = s.getStylesheet();
            if (stylesheet != null) {
                collectCustomProperties(stylesheet, customProperties);
            }
        }
        return customProperties;
    }

    private @NonNull Map<String, ImmutableList<CssToken>> collectCustomProperties(@NonNull Stylesheet s) {
        Map<String, ImmutableList<CssToken>> customProperties = new LinkedHashMap<>();
        collectCustomProperties(s, customProperties);
        return customProperties;
    }

    private void collectCustomProperties(@NonNull Stylesheet s, @NonNull Map<String, ImmutableList<CssToken>> customProperties) {
        for (StyleRule styleRule : s.getStyleRules()) {
            for (Declaration declaration : styleRule.getDeclarations()) {
                if (declaration.getPropertyName().startsWith("--")) {
                    customProperties.put(declaration.getPropertyName(), declaration.getTerms());
                }
            }
        }
    }

    private @NonNull ImmutableList<CssToken> preprocessTerms(E elem, @NonNull CssFunctionProcessor<E> processor, @NonNull ImmutableList<CssToken> terms) {
        try {
            return processor.process(elem, terms);
        } catch (ParseException e) {
            LOGGER.throwing(getClass().getName(), "error preprocessing token", e);
            return terms;
        }
    }

    protected class StylesheetEntry implements StylesheetInfo {

        private final @Nullable URI uri;
        private final @NonNull StyleOrigin origin;
        private @Nullable FutureTask<Stylesheet> future;
        private @Nullable Stylesheet stylesheet;


        public StylesheetEntry(@NonNull StyleOrigin origin, @NonNull URI uri) {
            this.origin = origin;
            this.uri = uri;
            this.future = new FutureTask<>(() -> {
                CssParser p = new CssParser();
                Stylesheet s = p.parseStylesheet(uri);
                LOGGER.info("Parsed " + uri + "\n#rules: " + s.getStyleRules().size() + ", #errors: " + p.getParseExceptions().size());
                List<ParseException> parseExceptions = p.getParseExceptions();
                if (!parseExceptions.isEmpty()) {
                    LOGGER.info("Parsed " + uri + "\nExceptions:\n  " + parseExceptions.stream().map(ParseException::getMessage).collect(Collectors.joining("\n  ")));
                }
                return s;
            });
            executor.execute(future);
        }

        public StylesheetEntry(@NonNull StyleOrigin origin, @NonNull Stylesheet stylesheet) {
            this.uri = null;
            this.origin = origin;
            this.stylesheet = stylesheet;
        }

        public StylesheetEntry(@NonNull StyleOrigin origin, @NonNull String str, @Nullable URI documentHome) {
            this.uri = null;
            this.origin = origin;
            this.future = new FutureTask<>(() -> {
                CssParser p = parserFactory.get();
                Stylesheet s = p.parseStylesheet(str, documentHome);
                LOGGER.info("Parsed " + str + "\nRules: " + s.getStyleRules());
                List<ParseException> parseExceptions = p.getParseExceptions();
                if (!parseExceptions.isEmpty()) {
                    LOGGER.info("Parsed " + str + "\nExceptions:\n  " + parseExceptions.stream().map(ParseException::getMessage).collect(Collectors.joining("\n  ")));
                }
                return s;
            });
            executor.execute(future);
        }

        @Override
        public @Nullable URI getUri() {
            return uri;
        }

        public @NonNull StyleOrigin getOrigin() {
            return origin;
        }

        public @Nullable Stylesheet getStylesheet() {
            if (future != null) {
                try {
                    stylesheet = future.get();
                    future = null;
                } catch (InterruptedException ex) {
                    // retry later
                } catch (ExecutionException ex) {
                    LOGGER.throwing(getClass().getName(), "getStylesheet", ex);
                    ex.printStackTrace();
                    stylesheet = null;
                    future = null;
                }
            }
            return stylesheet;
        }
    }

    @Override
    public List<StylesheetInfo> getStylesheets() {
        final ArrayList<StylesheetInfo> list = new ArrayList<>();
        list.addAll(userAgentList.values());
        list.addAll(authorList.values());
        list.addAll(inlineList.values());
        return list;
    }

    public Supplier<CssParser> getParserFactory() {
        return parserFactory;
    }

    public void setParserFactory(Supplier<CssParser> parserFactory) {
        this.parserFactory = parserFactory;
    }

    public UriResolver getUriResolver() {
        return uriResolver;
    }

    public void setUriResolver(UriResolver uriResolver) {
        this.uriResolver = uriResolver;
    }
}
