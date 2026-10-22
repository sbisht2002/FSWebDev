package com.fswebdev.calculator;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import java.math.BigDecimal;

public class MainActivity extends Activity {

    private static final String KEY_EXPRESSION = "expression";
    private static final String KEY_JUST_EVALUATED = "just_evaluated";

    private TextView expressionView;
    private TextView resultView;
    private HorizontalScrollView expressionScroll;

    private final StringBuilder expression = new StringBuilder();
    private boolean justEvaluated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        expressionView = (TextView) findViewById(R.id.expression);
        resultView = (TextView) findViewById(R.id.result);
        expressionScroll = (HorizontalScrollView) findViewById(R.id.expression_scroll);

        findViewById(R.id.key_back).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                clear();
                return true;
            }
        });

        if (savedInstanceState != null) {
            expression.append(savedInstanceState.getString(KEY_EXPRESSION, ""));
            justEvaluated = savedInstanceState.getBoolean(KEY_JUST_EVALUATED);
        }
        refresh();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_EXPRESSION, expression.toString());
        outState.putBoolean(KEY_JUST_EVALUATED, justEvaluated);
    }

    /** Click handler wired from the layout via android:onClick. */
    public void onKey(View v) {
        int id = v.getId();
        if (id == R.id.key_clear) {
            clear();
            return;
        } else if (id == R.id.key_back) {
            backspace();
        } else if (id == R.id.key_equals) {
            equals();
            return;
        } else if (id == R.id.key_dot) {
            dot();
        } else if (id == R.id.key_paren) {
            paren();
        } else if (id == R.id.key_percent) {
            percent();
        } else if (id == R.id.key_add) {
            operator('+');
        } else if (id == R.id.key_sub) {
            operator('−');
        } else if (id == R.id.key_mul) {
            operator('×');
        } else if (id == R.id.key_div) {
            operator('÷');
        } else {
            digit(((TextView) v).getText().charAt(0));
        }
        justEvaluated = false;
        refresh();
    }

    private char last() {
        return expression.length() == 0 ? '\0' : expression.charAt(expression.length() - 1);
    }

    private static boolean isOperator(char c) {
        return c == '+' || c == '−' || c == '×' || c == '÷';
    }

    private void startFreshIfEvaluated() {
        if (justEvaluated) {
            expression.setLength(0);
        }
    }

    private void digit(char d) {
        startFreshIfEvaluated();
        if (last() == ')' || last() == '%') {
            expression.append('×');
        }
        // Avoid leading zeros like "007".
        String number = currentNumber();
        if (number.equals("0")) {
            expression.setLength(expression.length() - 1);
        }
        expression.append(d);
    }

    private void dot() {
        startFreshIfEvaluated();
        String number = currentNumber();
        if (number.contains(".")) {
            return;
        }
        if (last() == ')' || last() == '%') {
            expression.append('×');
        }
        if (number.isEmpty()) {
            expression.append('0');
        }
        expression.append('.');
    }

    private void operator(char op) {
        char last = last();
        if (expression.length() == 0 || last == '(') {
            // Only a leading minus makes sense here.
            if (op == '−') {
                expression.append(op);
            }
            return;
        }
        if (isOperator(last)) {
            // Replace the previous operator, unless it is a lone unary minus.
            int len = expression.length();
            char before = len >= 2 ? expression.charAt(len - 2) : '\0';
            if (before == '\0' || before == '(') {
                return;
            }
            expression.setCharAt(len - 1, op);
            return;
        }
        if (last == '.') {
            expression.setLength(expression.length() - 1);
        }
        expression.append(op);
    }

    private void paren() {
        startFreshIfEvaluated();
        int open = 0;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(') open++;
            else if (c == ')') open--;
        }
        char last = last();
        boolean closesValue = Character.isDigit(last) || last == ')' || last == '%';
        if (open > 0 && closesValue) {
            expression.append(')');
        } else {
            if (closesValue || last == '.') {
                expression.append('×');
            }
            expression.append('(');
        }
    }

    private void percent() {
        char last = last();
        if (Character.isDigit(last) || last == ')') {
            expression.append('%');
        }
    }

    private void backspace() {
        if (justEvaluated) {
            return;
        }
        if (expression.length() > 0) {
            expression.setLength(expression.length() - 1);
        }
    }

    private void clear() {
        expression.setLength(0);
        justEvaluated = false;
        refresh();
    }

    private void equals() {
        if (expression.length() == 0 || justEvaluated) {
            return;
        }
        try {
            BigDecimal value = Evaluator.evaluate(expression.toString());
            expression.setLength(0);
            expression.append(Evaluator.format(value).replace('-', '−'));
            justEvaluated = true;
            expressionView.setText(expression);
            resultView.setText("");
        } catch (Evaluator.EvalException | ArithmeticException e) {
            resultView.setText(e.getMessage() != null ? e.getMessage() : "Error");
        }
    }

    /** Returns the trailing run of digits and '.' in the expression. */
    private String currentNumber() {
        int i = expression.length();
        while (i > 0) {
            char c = expression.charAt(i - 1);
            if (!Character.isDigit(c) && c != '.') break;
            i--;
        }
        return expression.substring(i);
    }

    private void refresh() {
        expressionView.setText(expression.length() == 0 ? "0" : expression);
        expressionScroll.post(new Runnable() {
            @Override
            public void run() {
                expressionScroll.fullScroll(View.FOCUS_RIGHT);
            }
        });
        resultView.setText(preview());
    }

    /** Live result shown under the expression while typing. */
    private String preview() {
        String expr = expression.toString();
        boolean hasOperation = false;
        for (int i = 1; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (isOperator(c) || c == '%' || c == '(') {
                hasOperation = true;
                break;
            }
        }
        if (!hasOperation || justEvaluated) {
            return "";
        }
        try {
            return "= " + Evaluator.format(Evaluator.evaluate(expr)).replace('-', '−');
        } catch (Evaluator.EvalException | ArithmeticException e) {
            return "";
        }
    }
}
