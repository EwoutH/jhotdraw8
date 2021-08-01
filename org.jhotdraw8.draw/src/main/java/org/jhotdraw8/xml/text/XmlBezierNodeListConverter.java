/*
 * @(#)XmlBezierNodeListConverter.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.xml.text;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.collection.ImmutableList;
import org.jhotdraw8.collection.ImmutableLists;
import org.jhotdraw8.css.CssTokenType;
import org.jhotdraw8.css.StreamCssTokenizer;
import org.jhotdraw8.geom.BezierNode;
import org.jhotdraw8.geom.BezierNodePath;
import org.jhotdraw8.geom.BezierNodePathBuilder;
import org.jhotdraw8.geom.SvgPaths;
import org.jhotdraw8.io.CharBufferReader;
import org.jhotdraw8.io.IdResolver;
import org.jhotdraw8.io.IdSupplier;
import org.jhotdraw8.text.Converter;

import java.io.IOException;
import java.nio.CharBuffer;
import java.text.ParseException;

/**
 * Converts an BezierNodeList path to a CSS String.
 * <p>
 * The null value will be converted to the CSS identifier "none".
 *
 * @author Werner Randelshofer
 */
public class XmlBezierNodeListConverter implements Converter<ImmutableList<BezierNode>> {

    private final boolean nullable;

    public XmlBezierNodeListConverter(boolean nullable) {
        this.nullable = nullable;
    }

    @Override
    public @Nullable ImmutableList<BezierNode> fromString(@NonNull CharBuffer buf, @Nullable IdResolver idResolver) throws ParseException, IOException {
        String input = buf.toString();
        StreamCssTokenizer tt = new StreamCssTokenizer(new CharBufferReader(buf));

        ImmutableList<BezierNode> p = null;
        if (tt.next() == CssTokenType.TT_IDENT) {
            if (!nullable) {
                throw new ParseException("String expected. " + tt.current(), buf.position());
            }
            if ("none".equals(tt.currentString())) {
                buf.position(buf.limit());

                return p;
            }
        }
        BezierNodePathBuilder builder = new BezierNodePathBuilder();
        SvgPaths.buildFromSvgString(builder, input);
        p = builder.build();

        buf.position(buf.limit());

        return p;
    }

    @Override
    public <TT extends ImmutableList<BezierNode>> void toString(@NonNull Appendable out, @Nullable IdSupplier idSupplier,
                                                                @Nullable TT value) throws IOException {
        if (value == null) {
            if (!nullable) {
                throw new IllegalArgumentException("value is null");
            }
            out.append("none");
            return;
        }

        out.append(SvgPaths.doubleSvgStringFromAwt(new BezierNodePath(value).getPathIterator(null)));// we lose smooth!

    }

    @Override
    public ImmutableList<BezierNode> getDefaultValue() {
        return ImmutableLists.emptyList();
    }
}
