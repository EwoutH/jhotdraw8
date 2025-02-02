/*
 * @(#)CssStrokeConverterTest.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.draw.css.text;

import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.base.converter.IdFactory;
import org.jhotdraw8.collection.immutable.ImmutableArrayList;
import org.jhotdraw8.css.parser.CssTokenType;
import org.jhotdraw8.css.value.CssSize;
import org.jhotdraw8.draw.css.CssStrokeStyle;
import org.jhotdraw8.draw.css.converter.CssStrokeStyleConverter;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * CssStrokeConverterTest.
 *
 * @author Werner Randelshofer
 */
public class CssStrokeConverterTest {

    private static final String IDENT_NONE = CssTokenType.IDENT_NONE;

    /**
     * Test of fromString method, of class CssStrokeStyleConverter.
     */
    public static void doTestFromString(CssStrokeStyle expected, @NonNull String string) throws Exception {
        CharBuffer buf = CharBuffer.wrap(string);
        IdFactory idFactory = null;
        CssStrokeStyleConverter instance = new CssStrokeStyleConverter(false);
        CssStrokeStyle actual = instance.fromString(buf, idFactory);
        assertEquals(expected, actual);
    }

    /**
     * Test of toString method, of class CssStrokeStyleConverter.
     */
    public static void doTestToString(CssStrokeStyle value, String expected) throws Exception {
        CssStrokeStyleConverter instance = new CssStrokeStyleConverter(false);
        String actual = instance.toString(value);
        assertEquals(expected, actual);
    }

    /**
     * Test of fromString and toString methods, of class CssStrokeStyleConverter.
     */
    public static void testStrokeStyle(CssStrokeStyle value, @NonNull String str) throws Exception {
        doTestFromString(value, str);
        doTestToString(value, str);
    }


    @TestFactory
    public @NonNull List<DynamicTest> dynamicTestsStrokeStyle() {
        return Arrays.asList(
                dynamicTest("1", () -> testStrokeStyle(
                        new CssStrokeStyle(),
                        "type(centered) linecap(butt) linejoin(miter) miterlimit(4) dashoffset(0) dasharray()")),
                dynamicTest("2", () -> testStrokeStyle(
                        new CssStrokeStyle(),
                        "type(centered) linecap(butt) linejoin(miter) miterlimit(4) dashoffset(0) dasharray()")),
                dynamicTest("3", () -> testStrokeStyle(
                        new CssStrokeStyle(StrokeType.CENTERED, StrokeLineCap.ROUND, StrokeLineJoin.MITER, CssSize.from(3)
                                , CssSize.from(4), ImmutableArrayList.of(CssSize.from(5), CssSize.from(6))),
                        "type(centered) linecap(round) linejoin(miter) miterlimit(3) dashoffset(4) dasharray(5 6)")),
                dynamicTest("4", () -> testStrokeStyle(
                        new CssStrokeStyle(StrokeType.CENTERED, StrokeLineCap.BUTT, StrokeLineJoin.MITER, CssSize.from(3)
                                , CssSize.from(4), ImmutableArrayList.of(CssSize.from(5), CssSize.from(6))),
                        "type(centered) linecap(butt) linejoin(miter) miterlimit(3) dashoffset(4) dasharray(5 6)")),
                dynamicTest("5", () -> testStrokeStyle(
                        new CssStrokeStyle(StrokeType.INSIDE, StrokeLineCap.ROUND, StrokeLineJoin.MITER, CssSize.from(3)
                                , CssSize.from(4), ImmutableArrayList.of(CssSize.from(5), CssSize.from(6))),
                        "type(inside) linecap(round) linejoin(miter) miterlimit(3) dashoffset(4) dasharray(5 6)")),
                dynamicTest("6", () -> testStrokeStyle(
                        new CssStrokeStyle(StrokeType.CENTERED, StrokeLineCap.BUTT, StrokeLineJoin.MITER, CssSize.from(4)
                                , CssSize.from(0), ImmutableArrayList.of()),
                        "type(centered) linecap(butt) linejoin(miter) miterlimit(4) dashoffset(0) dasharray()"))
        );
    }

}