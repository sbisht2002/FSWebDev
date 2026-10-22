package com.fswebdev.calculator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Recursive-descent evaluator for calculator expressions.
 *
 * Grammar:
 *   expr    := term (('+' | '-') term)*
 *   term    := unary (('*' | '/') unary | implicit-multiply)*
 *   unary   := ('+' | '-') unary | postfix
 *   postfix := primary '%'*
 *   primary := number | '(' expr ')'
 *
 * Accepts the display symbols × ÷ − as well as their ASCII forms.
 */
public final class Evaluator {

    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_EVEN);
    private static final MathContext DISPLAY = new MathContext(12, RoundingMode.HALF_EVEN);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static final class EvalException extends Exception {
        public EvalException(String message) {
            super(message);
        }
    }

    private final String src;
    private int pos;

    private Evaluator(String src) {
        this.src = normalize(src);
    }

    /** Evaluates the expression, auto-closing any unbalanced parentheses. */
    public static BigDecimal evaluate(String expression) throws EvalException {
        Evaluator e = new Evaluator(expression);
        if (e.src.isEmpty()) {
            throw new EvalException("Empty");
        }
        BigDecimal value = e.parseExpr();
        if (e.pos < e.src.length()) {
            throw new EvalException("Unexpected '" + e.src.charAt(e.pos) + "'");
        }
        return value;
    }

    /** Formats a result for display: up to 12 significant digits, no trailing zeros. */
    public static String format(BigDecimal value) {
        BigDecimal rounded = value.round(DISPLAY).stripTrailingZeros();
        if (rounded.signum() == 0) {
            return "0";
        }
        BigDecimal abs = rounded.abs();
        if (abs.compareTo(new BigDecimal("1e12")) >= 0 || abs.compareTo(new BigDecimal("1e-9")) < 0) {
            return rounded.toString().replace("E+", "e").replace("E", "e");
        }
        return rounded.toPlainString();
    }

    private static String normalize(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 4);
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '×': sb.append('*'); break;
                case '÷': sb.append('/'); break;
                case '−': sb.append('-'); break;
                case ' ': break;
                case '(': depth++; sb.append(c); break;
                case ')': depth--; sb.append(c); break;
                default: sb.append(c);
            }
        }
        while (depth-- > 0) {
            sb.append(')');
        }
        return sb.toString();
    }

    private char peek() {
        return pos < src.length() ? src.charAt(pos) : '\0';
    }

    private BigDecimal parseExpr() throws EvalException {
        BigDecimal value = parseTerm();
        while (true) {
            char c = peek();
            if (c == '+') {
                pos++;
                value = value.add(parseTerm(), MC);
            } else if (c == '-') {
                pos++;
                value = value.subtract(parseTerm(), MC);
            } else {
                return value;
            }
        }
    }

    private BigDecimal parseTerm() throws EvalException {
        BigDecimal value = parseUnary();
        while (true) {
            char c = peek();
            if (c == '*') {
                pos++;
                value = value.multiply(parseUnary(), MC);
            } else if (c == '/') {
                pos++;
                BigDecimal divisor = parseUnary();
                if (divisor.signum() == 0) {
                    throw new EvalException("Can't divide by 0");
                }
                value = value.divide(divisor, MC);
            } else if (c == '(' || Character.isDigit(c) || c == '.') {
                // Implicit multiplication, e.g. 2(3) or (1+2)(4).
                value = value.multiply(parseUnary(), MC);
            } else {
                return value;
            }
        }
    }

    private BigDecimal parseUnary() throws EvalException {
        char c = peek();
        if (c == '-') {
            pos++;
            return parseUnary().negate();
        }
        if (c == '+') {
            pos++;
            return parseUnary();
        }
        return parsePostfix();
    }

    private BigDecimal parsePostfix() throws EvalException {
        BigDecimal value = parsePrimary();
        while (peek() == '%') {
            pos++;
            value = value.divide(HUNDRED, MC);
        }
        return value;
    }

    private BigDecimal parsePrimary() throws EvalException {
        char c = peek();
        if (c == '(') {
            pos++;
            BigDecimal value = parseExpr();
            if (peek() != ')') {
                throw new EvalException("Missing ')'");
            }
            pos++;
            return value;
        }
        int start = pos;
        while (Character.isDigit(peek()) || peek() == '.') {
            pos++;
        }
        if (start == pos) {
            throw new EvalException(c == '\0' ? "Incomplete expression" : "Unexpected '" + c + "'");
        }
        try {
            return new BigDecimal(src.substring(start, pos));
        } catch (NumberFormatException ex) {
            throw new EvalException("Bad number");
        }
    }
}
