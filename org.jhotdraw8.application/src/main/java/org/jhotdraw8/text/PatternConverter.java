/*
 * @(#)PatternConverter.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.text;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.io.IdResolver;
import org.jhotdraw8.io.IdSupplier;
import org.jhotdraw8.io.SimpleIdFactory;
import org.jhotdraw8.io.StreamPosTokenizer;

import java.io.IOException;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts an object array from or to a string representation.
 * <p>
 * The string representation must be described using a pattern.
 * <p>
 * The pattern has the following format:
 * <pre>
 * <i>TextFormatPattern:</i>
 *       <i>PatternElement</i>
 *       <i>PatternElement TextFormatPattern</i>
 *
 * <i>PatternElement:</i>
 *       <i>Regex</i>
 *       <i>Argument</i>
 *
 * <i>Argument:</i>
 *       { <i>ArgumentIndex</i> }
 *       { <i>ArgumentIndex</i> , <i>FormatType</i> }
 *       { <i>ArgumentIndex</i> , <i>FormatType</i> , <i>FormatStyle</i> }
 *
 * <i>Regex:</i>
 *       <i>RegexChars RegexRepeat</i>
 *       <i>RegexCharclass RegexRepeat</i>
 *       <i>RegexChoice RegexRepeat</i>
 *
 * <i>RegexChars:</i>
 *       <i>Char</i>
 *       <i>QuotedChars</i>
 *
 * <i>RegexRepeat:</i>
 *       *
 *       +
 *       <i>empty</i>
 *
 * <i>RegexCharclass:</i>
 *       [ <i>Chars</i> ]
 *
 * <i>RegexChoice:</i>
 *       ( <i>RegexChoiceList</i> )
 *
 * <i>RegexChoiceList:</i>
 *       <i>Regex</i>
 *       <i>Regex</i> | <i>RegexChoiceList</i>
 *
 * <i>FormatType:</i>
 *       list
 *       choice
 *       <i>FormatTypeSupportedByConverterFactory</i>
 *
 * <i>FormatStyle:</i>
 *       <i>ListPattern</i>
 *       <i>ChoicePattern</i>
 *       <i>FormatStyleSupportedByConverterFactory</i>
 *
 * <i>ListPattern:</i>
 *       <i>ItemPattern | SeparatorPattern </i>
 *
 * <i>ItemPattern:</i>
 * <i>SeparatorPattern:</i>
 *       <i>TextFormatPattern</i>
 *
 * <i>ChoicePattern:</i>
 *       <i>Limit</i> # <i>TextFormatPattern</i>
 *       <i>Limit</i> # <i>TextFormatPattern</i> | <i>ChoicePattern</i>
 *
 * <i>Limit: a double number</i>
 * </pre>
 * <p>
 * Within a {@code String}, a pair of single quotes can be used to quote any
 * arbitrary characters except single quotes. For example, pattern string
 * {@code "'{0}'"} represents string {@code "{0}"}, not a FormatElement. A
 * single quote itself must be represented by doubled single quotes {@code ''}.
 * <p>
 * Any curly braces within {@code FormatType} and {@code FormatStyle} must be
 * balanced.
 * </p>
 * <p>
 * The {@code ChoicePattern} works like in {@code java.text.ChoiceFormat}. The
 * choice is specified with an ascending list of doubles, where each item
 * specifies a half-open interval up to the next item:
 * </p>
 * <pre>
 * X matches j if and only if limit[j] ≤ X &lt; limit[j+1]
 * </pre>
 * <p>
 * If there is no match, then either the first or last index is used, depending
 * on whether the number (X) is too low or too high. If the limit array is not
 * in ascending order, the results of formatting will be incorrect. ChoiceFormat
 * also accepts {@code \u221E} as equivalent to infinity(INF).
 * </p>
 * <p>
 * If the separator of a list contains arguments, then their argument indices
 * should be smaller than the argument index.
 * </p>
 *
 * @author Werner Randelshofer
 */
public class PatternConverter implements Converter<Object[]> {

    private final AST ast;
    private final ConverterFactory factory;
    /**
     * Number of argument indices needed.
     */
    private final int numIndices;

    public PatternConverter(@NonNull String pattern, ConverterFactory factory) {
        try {
            ast = parseTextFormatPattern(pattern);
            numIndices = 1 + ast.getMaxArgumentIndex();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Illegal pattern", ex);
        }

        this.factory = factory;
    }

    public void toStr(Appendable out, IdSupplier idSupplier, Object... value) throws IOException {
        toString(out, idSupplier, value);
    }

    public @NonNull String format(Object... value) {
        StringBuilder buf = new StringBuilder();
        try {
            toString(buf, new SimpleIdFactory(), value);
        } catch (IOException ex) {
            throw new InternalError(ex);
        }
        return buf.toString();
    }

    @Override
    public void toString(Appendable out, @Nullable IdSupplier idSupplier, Object[] value) throws IOException {
        int[] indices = new int[numIndices];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        ast.toString(value, out, factory, indices);
    }

    @Override
    public @NonNull Object[] fromString(@NonNull CharBuffer buf, @Nullable IdResolver idResolver) throws ParseException, IOException {
        int[] indices = new int[numIndices];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        ArrayList<Object> value = new ArrayList<>();
        ast.fromString(buf, factory, value, indices);
        return value.toArray();
    }

    /**
     * Pattern AST. This class is package visible for testing purposes.
     */
    static class AST {

        protected @NonNull List<AST> children = new ArrayList<>();

        @Override
        public @NonNull String toString() {
            return "AST{" + children + '}';
        }

        public void toString(Object[] value, Appendable out, ConverterFactory factory, int[] indices) throws IOException {
            for (AST child : children) {
                child.toString(value, out, factory, indices);
            }
        }

        public int getMaxArgumentIndex() {
            int index = -1;
            for (AST child : children) {
                index = Math.max(index, +child.getMaxArgumentIndex());
            }
            return index;
        }

        public void fromString(CharBuffer buf, ConverterFactory factory, ArrayList<Object> value, int[] indices) throws IOException, ParseException {
            for (AST child : children) {
                child.fromString(buf, factory, value, indices);
            }
        }

        static @NonNull String escape(int charAt) {
            if (charAt == -1) {
                return "end of file";
            }
            if (Character.isISOControl((char) charAt)) {
                String hex = "000" + Integer.toHexString(charAt);
                return "\\u" + hex.substring(hex.length() - 4);
            } else {
                return String.valueOf((char) charAt);
            }
        }

        static @NonNull String escape(@NonNull String str) {
            StringBuilder buf = new StringBuilder(str.length());
            for (int i = 0, n = str.length(); i < n; i++) {
                char ch = str.charAt(i);
                if (Character.isISOControl(ch)) {
                    String hex = "000" + Integer.toHexString(ch);
                    buf.append("\\u").append(hex.substring(hex.length() - 4));
                } else {
                    buf.append(ch);
                }
            }
            return buf.toString();
        }
    }

    abstract static class Argument extends AST {

        protected int index;

        @Override
        public int getMaxArgumentIndex() {
            return Math.max(index, super.getMaxArgumentIndex());
        }
    }

    static class SimpleArgument extends Argument {

        protected String type;
        protected String style;
        protected Converter<Object> converter;

        @Override
        public @NonNull String toString() {
            if (type.isEmpty() && style.isEmpty()) {
                return "Arg{" + index + '}';
            } else {
                return "Arg{" + index + " type=" + type
                        + " style=" + style + '}';
            }
        }

        @Override
        public void toString(Object[] value, Appendable out, @NonNull ConverterFactory factory, int[] indices) throws IOException {
            if (converter == null) {
                @SuppressWarnings("unchecked")
                Converter<Object> temp = (Converter<Object>) factory.apply(type, style);
                converter = temp;
            }
            converter.toString(out, value[indices[index]]);
        }

        @Override
        public void fromString(CharBuffer buf, @NonNull ConverterFactory factory, @NonNull ArrayList<Object> value, int[] indices) throws IOException, ParseException {
            if (converter == null) {
                @SuppressWarnings("unchecked")
                Converter<Object> temp = (Converter<Object>) factory.apply(type, style);
                converter = temp;
            }
            Object v = converter.fromString(buf);
            while (value.size() <= indices[index]) {
                value.add(null);
            }
            value.set(indices[index], v);
//            converter.toString(value[indices[index]], out);
        }

    }

    /**
     * Each child represents a choice.
     */
    static class ChoiceArgument extends Argument {

        protected double[] limits;

        @Override
        public @NonNull String toString() {
            return "ArgChoice{" + index + " limits=" + Arrays.toString(limits)
                    + "' children=" + children
                    + '}';
        }

        @Override
        public void toString(Object[] value, Appendable out, ConverterFactory factory, int[] indices) throws IOException {
            //int choiceIndex = Collections.binarySearch(limits, ((Number) value[index]).doubleValue());
            int choiceIndex = Arrays.binarySearch(limits, ((Number) value[index]).doubleValue());
            if (choiceIndex < 0) {
                choiceIndex = -choiceIndex - 1;
            }
            if (choiceIndex >= limits.length) {
                choiceIndex = limits.length - 1;
            }

            children.get(choiceIndex).toString(value, out, factory, indices);
        }

        @Override
        public void fromString(@NonNull CharBuffer buf, ConverterFactory factory, @NonNull ArrayList<Object> value, int[] indices) throws IOException, ParseException {
            int pos = buf.position();
            int choice = -1;
            int greediest = -1;
            for (int i = 0, n = children.size(); i < n; i++) {
                buf.position(pos);
                AST child = children.get(i);

                // try to parse each choice, take the greediest one
                try {
                    child.fromString(buf, factory, value, indices);
                    if (buf.position() > greediest) {
                        choice = i;
                        greediest = buf.position();
                    }
                } catch (ParseException e) {
                    // empty because we try again with a different choice
                    //e.printStackTrace();
                }
            }
            if (greediest > 0) {
                buf.position(greediest);
            }
            while (value.size() <= index) {
                value.add(null);
            }
            value.set(index, limits[choice]);
        }

    }

    /**
     * First child represents item, second child represents separator.
     */
    static class ListArgument extends Argument {

        protected int maxIndex;

        @Override
        public @NonNull String toString() {
            return "ArgList{" + index + " children=" + children
                    + '}';
        }

        @Override
        public void toString(Object[] value, Appendable out, ConverterFactory factory, @NonNull int[] indices) throws IOException {
            int i = indices[index];

            AST separator = children.size() < 2 ? null : children.get(1);
            AST child = children.get(0);

            int step = maxIndex - index;
            int repeat = ((Number) value[i]).intValue();
            // update indices after list
            int shift = step * (repeat - 1);
            for (int j = maxIndex + 1; j < indices.length; j++) {
                indices[j] += shift;
            }

            // process list items
            for (int j = 0; j < repeat; j++) {
                if (j != 0 && separator != null) {
                    separator.toString(value, out, factory, indices);
                }

                child.toString(value, out, factory, indices);

                // update list item indices
                for (int k = index + 1; k <= maxIndex; k++) {
                    indices[k] += step;
                }
            }
        }

        @Override
        public void fromString(CharBuffer buf, ConverterFactory factory, @NonNull ArrayList<Object> value, @NonNull int[] indices) throws IOException, ParseException {
            int i = indices[index];

            AST separator = children.size() < 2 ? null : children.get(1);
            AST child = children.get(0);

            int step = maxIndex - index;
            int repeat = 0;

            // process list items
            try {
                for (int j = 0; true; j++) {
                    if (buf.remaining() == 0) {
                        break;
                    }
                    if (j != 0 && separator != null) {
                        separator.fromString(buf, factory, value, indices);
                    }
                    child.fromString(buf, factory, value, indices);
                    repeat++;

                    // update list item indices
                    for (int k = index + 1; k <= maxIndex; k++) {
                        indices[k] += step;
                    }
                }
            } catch (ParseException e) {
                // empty because we reached the end of the list
            }

            // update indices after list
            int shift = step * (repeat - 1);
            for (int j = maxIndex + 1; j < indices.length; j++) {
                indices[j] += shift;
            }

            while (value.size() <= i) {
                value.add(null);
            }
            value.set(i, repeat);
        }

    }

    abstract static class Regex extends AST {

        protected int maxRepeat;
        protected int minRepeat;
    }

    static class RegexChars extends Regex {

        protected @Nullable String chars;

        @Override
        public @NonNull String toString() {
            if (minRepeat == 1 && maxRepeat == 1) {
                return "\"" + chars + "\"";
            } else {
                return "\"" + chars + "\"{" + minRepeat + "," + maxRepeat + "}";
            }
        }

        @Override
        public void toString(Object[] value, @NonNull Appendable out, ConverterFactory factory, int[] indices) throws IOException {
            for (int i = 0; i < minRepeat; i++) {
                out.append(chars);
            }
        }

        @Override
        public void fromString(@NonNull CharBuffer buf, ConverterFactory factory, ArrayList<Object> value, int[] indices) throws IOException, ParseException {
            for (int i = 0; i < maxRepeat; i++) {
                int reset = buf.position();
                for (int j = 0; j < chars.length(); j++) {
                    int ch = buf.remaining() > 0 ? buf.get() : -1;
                    if (ch != chars.charAt(j)) {
                        if (i < minRepeat) {
                            throw new ParseException("Expected character '"
                                    + escape(chars.charAt(j)) + "' but found '"
                                    + escape(ch) + "'.", buf.position());
                        } else {
                            buf.position(reset);
                            return;
                        }
                    }
                }
            }
        }

    }

    static class RegexCharclass extends Regex {

        protected @Nullable String chars;

        @Override
        public @NonNull String toString() {
            return "RegCharclass[" + chars + "] repeat=" + minRepeat
                    + ".." + maxRepeat + '}';
        }

        @Override
        public void toString(Object[] value, @NonNull Appendable out, ConverterFactory factory, int[] indices) throws IOException {
            for (int i = 0; i < minRepeat; i++) {
                out.append(chars.charAt(0));
            }
        }

        @Override
        public void fromString(@NonNull CharBuffer buf, ConverterFactory factory, ArrayList<Object> value, int[] indices) throws IOException, ParseException {
            for (int i = 0; i < maxRepeat; i++) {
                int reset = buf.position();
                int ch = buf.remaining() > 0 ? buf.get() : -1;
                boolean found = false;
                for (int j = 0; j < chars.length(); j++) {
                    if (ch == chars.charAt(j)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    if (i < minRepeat) {
                        throw new ParseException("Expected character in ["
                                + escape(chars) + "] but found '"
                                + (ch == -1 ? "<EOF>" : (char) ch) + "'. Min Repeat=" + minRepeat, buf.position());
                    } else {
                        buf.position(reset);
                        return;
                    }
                }
            }
        }
    }

    static class RegexChoice extends Regex {

        @Override
        public @NonNull String toString() {
            return "RegChoice{children='" + children + " repeat=" + minRepeat
                    + ".." + maxRepeat + '}';
        }

        @Override
        public void toString(Object[] value, Appendable out, ConverterFactory factory, int[] indices) throws IOException {
            for (int i = 0; i < minRepeat; i++) {
                children.get(0).toString(value, out, factory, indices);
            }
        }

        @Override
        public void fromString(@NonNull CharBuffer buf, ConverterFactory factory, ArrayList<Object> value, int[] indices) throws IOException, ParseException {
            for (int i = 0; i < maxRepeat; i++) {
                int reset = buf.position();
                for (int j = 0, n = children.size(); j < n; j++) {
                    AST child = children.get(j);

                    // try to parse each choice, break on success
                    try {
                        child.fromString(buf, factory, value, indices);
                        break;
                    } catch (ParseException e) {
                        if (j < n - 1) {// reset position since more choices are left
                            buf.position(reset);
                        } else// fail since we ran out of choices
                            if (i < minRepeat) {
                                throw new ParseException("Could not parse choice.", reset);
                            } else {
                                buf.position(reset);
                                return;
                            }
                    }
                }
            }
        }

    }

    static @NonNull AST parseTextFormatPattern(@NonNull String pattern) throws IOException {
        return parseTextFormatPattern(pattern, new AST(), 0);
    }

    private static @NonNull AST parseTextFormatPattern(@NonNull String pattern, @NonNull AST parent, int offset) throws IOException {
        StreamPosTokenizer tt = new StreamPosTokenizer(new StringReader(pattern));
        tt.resetSyntax();
        tt.quoteChar('\'');

        parseTextFormatPattern(tt, parent, offset);
        return parent;
    }

    private static void parseTextFormatPattern(@NonNull StreamPosTokenizer tt, @NonNull AST parent, int offset) throws IOException {
        while (tt.nextToken() != StreamPosTokenizer.TT_EOF) {
            tt.pushBack();
            parsePatternElement(tt, parent, offset);
        }
    }

    private static void parsePatternElement(@NonNull StreamPosTokenizer tt, @NonNull AST parent, int offset) throws IOException {
        switch (tt.nextToken()) {
        case StreamPosTokenizer.TT_EOF:
            return;
        case '{':
            tt.pushBack();
            parseArgument(tt, parent, offset);
            break;
        default:
            tt.pushBack();
            parseRegex(tt, parent, offset);
            break;
        }
    }

    private static void parseRegex(@NonNull StreamPosTokenizer tt, @NonNull AST parent, int offset) throws IOException {
        switch (tt.nextToken()) {
        case StreamPosTokenizer.TT_EOF:
            throw new IOException("RegexExpression expected @"
                    + (tt.getStartPosition() + offset));

        case '(':
            tt.pushBack();
            parseRegexChoice(tt, parent, offset);
            break;
        case '[':
            tt.pushBack();
            parseRegexCharclass(tt, parent, offset);
            break;
        case '+':
        case '*':
        case ')':
        case ']':
            throw new IOException("RegexExpression may not start with '"
                    + (char) tt.ttype + "' @"
                    + (tt.getStartPosition() + offset));
        case '\'':
        default:
            tt.pushBack();
            parseRegexChars(tt, parent, offset);
            break;
        }
    }

    private static void parseRegexRepeat(@NonNull StreamPosTokenizer tt, @NonNull Regex regex, int offset) throws IOException {
        switch (tt.nextToken()) {
        case '+':
            regex.minRepeat = 1;
            regex.maxRepeat = Integer.MAX_VALUE;
            break;
        case '*':
            regex.minRepeat = 0;
            regex.maxRepeat = Integer.MAX_VALUE;
            break;
        default:
            regex.minRepeat = 1;
            regex.maxRepeat = 1;
            tt.pushBack();
            break;
        }
    }

    private static void parseRegexChars(@NonNull StreamPosTokenizer tt, @NonNull AST parent, int offset) throws IOException {
        RegexChars regex = new RegexChars();
        regex.chars = "";
        switch (tt.nextToken()) {
        case StreamPosTokenizer.TT_EOF:
            throw new IOException("RegexChars expected @"
                    + (tt.getStartPosition() + offset));
        case '\'':
            regex.chars += tt.sval.isEmpty() ? "'" : tt.sval;

            break;
        case '(':
        case '[':
        case '+':
        case '*':
        case ')':
        case ']':
            throw new IOException("RegexChars may not start with '"
                    + (char) tt.ttype + "' @"
                    + (tt.getStartPosition() + offset));

        default:
            regex.chars += (char) tt.ttype;
            break;
        }
        parseRegexRepeat(tt, regex, offset);
        parent.children.add(regex);
    }

    private static void parseRegexChoice(@NonNull StreamPosTokenizer tt, @NonNull AST parent, int offset) throws IOException {
        RegexChoice regex = new RegexChoice();
        if (tt.nextToken() != '(') {
            throw new IOException("RegexChoice '(' expected @"
                    + (tt.getStartPosition() + offset));
        }
        do {
            parseRegex(tt, regex, offset);
        } while (tt.nextToken() == '|');
        if (tt.ttype != ')') {
            throw new IOException("RegexChoice ')' expected @"
                    + (tt.getStartPosition() + offset));
        }
        parseRegexRepeat(tt, regex, offset);
        parent.children.add(regex);
    }

    private static void parseRegexCharclass(@NonNull StreamPosTokenizer tt, @NonNull AST parent, int offset) throws IOException {
        RegexCharclass regex = new RegexCharclass();
        if (tt.nextToken() != '[') {
            throw new IOException("RegexCharclass '[' expected @"
                    + (tt.getStartPosition() + offset));
        }
        regex.chars = "";
        while (tt.nextToken() != ']') {
            tt.pushBack();
            switch (tt.nextToken()) {
            case StreamPosTokenizer.TT_EOF:
                throw new IOException("RegexCharclass character expected @"
                        + (tt.getStartPosition() + offset));
            case '\'':
                regex.chars += (tt.sval.isEmpty()) ? "\'" : tt.sval;
                break;
            default:
                regex.chars += (char) tt.ttype;
                break;
            }
        }
        if (regex.chars.isEmpty()) {
            throw new IOException("RegexCharclass illegal empty character class @"
                    + (tt.getStartPosition() + offset));

        }
        parseRegexRepeat(tt, regex, offset);
        parent.children.add(regex);
    }

    private static void parseArgument(@NonNull StreamPosTokenizer tt, @NonNull AST parent, int offset) throws IOException {
        if (tt.nextToken() != '{') {
            throw new IOException("Argument '{' expected @"
                    + (tt.getStartPosition() + offset));
        }
        if (tt.nextToken() == '}') {
            throw new IOException("Argument unexpected '}' expected @"
                    + (tt.getStartPosition() + offset));
        }
        tt.pushBack();

        // parse argument index
        int index = 0;
        while (tt.nextToken() != ',' && tt.ttype != '}' && tt.ttype
                != StreamPosTokenizer.TT_EOF) {

            switch (tt.ttype) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                index = index * 10 + tt.ttype - '0';
                break;
            default:
                throw new IOException("argument index is not numeric @" + (tt.getStartPosition() + offset));
            }
        }
        // parse argument type
        StringBuilder type = new StringBuilder();
        if (tt.ttype == ',') {
            while (tt.nextToken() != ',' && tt.ttype != '}' && tt.ttype
                    != StreamPosTokenizer.TT_EOF) {

                switch (tt.ttype) {
                case '\'':
                    type.append((tt.sval.isEmpty()) ? "\'" : tt.sval);
                    break;
                default:
                    type.append((char) tt.ttype);
                    break;
                }
            }
        }
        if (tt.ttype != ',') {
            tt.pushBack();
        }
        String typeStr = type.toString();

        switch (typeStr) {
        case "choice":
            parseChoiceArgumentStyle(tt, parent, index, offset);
            break;
        case "list":
            parseListArgumentStyle(tt, parent, index, offset);
            break;
        default:
            parseSimpleArgumentStyle(tt, parent, index, typeStr, offset);
            break;
        }
    }

    private static void parseSimpleArgumentStyle(@NonNull StreamPosTokenizer tt, @NonNull AST parent, int index, String type, int offset) throws IOException {
        // parse argument style
        StringBuilder style = new StringBuilder();

        // parse item
        int depth = 0;
        while ((tt.nextToken() != '}' || depth > 0) && tt.ttype
                != StreamPosTokenizer.TT_EOF) {

            switch (tt.ttype) {
            case '\'':
                style.append((tt.sval.isEmpty()) ? "\'" : tt.sval);
                break;
            case '{':
                style.append('{');
                depth++;
                break;
            case '}':
                style.append('}');
                depth--;
                break;
            default:
                style.append((char) tt.ttype);
                break;
            }
        }

        SimpleArgument argument = new SimpleArgument();
        argument.index = index;
        argument.type = type;
        argument.style = style.toString();
        parent.children.add(argument);
    }

    private static void parseChoiceArgumentStyle(@NonNull StreamPosTokenizer tt, @NonNull AST parent, int index, int offset) throws IOException {
        ChoiceArgument argument = new ChoiceArgument();
        argument.index = index;
        ArrayList<Double> limits = new ArrayList<>();
        // parse argument style
        StringBuilder style = new StringBuilder();

        int depth = 0;
        while ((tt.nextToken() != '}' || depth > 0) && tt.ttype
                != StreamPosTokenizer.TT_EOF) {

            tt.pushBack();

            // parse limit
            int startPosition = tt.getStartPosition();
            while (((tt.nextToken() != '#' && tt.ttype != '}') || depth > 0)
                    && tt.ttype != StreamPosTokenizer.TT_EOF) {
                switch (tt.ttype) {
                case '\'':
                    style.append((tt.sval.isEmpty()) ? "\'" : tt.sval);
                    break;
                default:
                    style.append((char) tt.ttype);
                    break;
                }
            }
            try {
                double limit = Double.parseDouble(style.toString());
                limits.add(limit);
            } catch (NumberFormatException e) {
                throw new IOException("Choice Argument: Illegal number format for limit: '"
                        + style + "' @" + (startPosition + offset));
            }
            style.delete(0, style.length());
            if (tt.ttype != '#') {
                tt.pushBack();
            }

            // parse item
            int itemPosition = tt.getEndPosition();
            while (((tt.nextToken() != '|' && tt.ttype != '}') || depth > 0)
                    && tt.ttype != StreamPosTokenizer.TT_EOF) {
                switch (tt.ttype) {
                case '\'':
                    style.append((tt.sval.isEmpty()) ? "\'" : tt.sval);
                    break;
                case '{':
                    style.append('{');
                    depth++;
                    break;
                case '}':
                    style.append('}');
                    depth--;
                    break;
                default:
                    style.append((char) tt.ttype);
                    break;
                }
            }
            AST child = new AST();
            argument.limits = new double[limits.size()];
            for (int i = 0, n = limits.size(); i < n; i++) {
                argument.limits[i] = limits.get(i);
            }
            argument.children.add(child);
            parseTextFormatPattern(style.toString(), child, offset
                    + itemPosition);
            style.delete(0, style.length());
            if (tt.ttype != '|') {
                tt.pushBack();
            }
        }

        parent.children.add(argument);
    }

    private static void parseListArgumentStyle(@NonNull StreamPosTokenizer tt, @NonNull AST parent, int index, int offset) throws IOException {
        ListArgument argument = new ListArgument();
        argument.index = index;
        // parse argument style
        StringBuilder style = new StringBuilder();

        int depth = 0;
        while ((tt.nextToken() != '}' || depth > 0) && tt.ttype
                != StreamPosTokenizer.TT_EOF) {

            tt.pushBack();

            // parse item
            while (((tt.nextToken() != '|' && tt.ttype != '}') || depth > 0)
                    && tt.ttype != StreamPosTokenizer.TT_EOF) {
                switch (tt.ttype) {
                case '\'':
                    style.append((tt.sval.isEmpty()) ? "\'" : tt.sval);
                    break;
                case '{':
                    style.append('{');
                    depth++;
                    break;
                case '}':
                    style.append('}');
                    depth--;
                    break;
                default:
                    style.append((char) tt.ttype);
                    break;
                }
            }
            AST child = new AST();
            argument.children.add(child);
            parseTextFormatPattern(style.toString(), child, offset
                    + tt.getEndPosition());
            style.delete(0, style.length());
            if (tt.ttype != '|') {
                tt.pushBack();
            }
            argument.maxIndex = Math.max(argument.maxIndex, child.getMaxArgumentIndex());
        }

        parent.children.add(argument);
    }

    @Override
    public @NonNull Object[] getDefaultValue() {
        return new Object[0];
    }
}
