/*
 * @(#)CssNumberConverter.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.css.text;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.css.CssToken;
import org.jhotdraw8.css.CssTokenType;
import org.jhotdraw8.css.CssTokenizer;
import org.jhotdraw8.io.IdResolver;
import org.jhotdraw8.io.IdSupplier;

import java.io.IOException;
import java.text.ParseException;
import java.util.function.Consumer;

/**
 * CssNumberConverter.
 * <p>
 * Parses the following EBNF:
 * <pre>
 * Number := number-token | "-INF" | "INF" | "NaN";
 * number-token = (* CSS number-token *)
 * </pre>
 * <p>
 * References:
 * <dl>
 *     <dt>CSS Syntax Module Level 3, 4. Token Railroad Diagrams, Number Token Diagram</dt>
 *     <dd><a href="">w3.org</a>https://www.w3.org/TR/css-syntax-3/#number-token-diagram</dd>
 * </dl>
 *
 * @author Werner Randelshofer
 */
public class CssNumberConverter extends AbstractCssConverter<Number> {

    public CssNumberConverter(boolean nullable) {
        super(nullable);
    }

    @Override
    public @NonNull Number parseNonNull(@NonNull CssTokenizer tt, @Nullable IdResolver idResolver) throws ParseException, IOException {
        switch (tt.next()) {
        case CssTokenType.TT_NUMBER:
            return tt.currentNumberNonNull();
        case CssTokenType.TT_IDENT: {
            double value;
            switch (tt.currentStringNonNull()) {
            case "INF":
                value = Double.POSITIVE_INFINITY;
                break;
            case "-INF":
                value = Double.NEGATIVE_INFINITY;
                break;
            case "NaN":
                value = Double.NaN;
                break;
            default:
                throw new ParseException("number expected:" + tt.currentString(), tt.getStartPosition());
            }
            return value;
        }
        default:
            throw new ParseException("⟨Double⟩: number expected.", tt.getStartPosition());
        }
    }

    @Override
    public <TT extends Number> void produceTokensNonNull(@NonNull TT value, @Nullable IdSupplier idSupplier, @NonNull Consumer<CssToken> out) {
        out.accept(new CssToken(CssTokenType.TT_NUMBER, value));
    }

    @Override
    public @NonNull String getHelpText() {
        return "Format of ⟨Number⟩: ⟨number⟩";
    }

}
