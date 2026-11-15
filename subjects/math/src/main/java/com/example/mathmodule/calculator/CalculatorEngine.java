package com.example.mathmodule.calculator;

import java.math.BigDecimal;
import java.math.MathContext;

/** Scientific expression evaluator for the in-lesson calculator. */
public final class CalculatorEngine {
  private final String text;
  private final boolean degrees;
  private final double answer;
  private final double memory;
  private int position;

  private CalculatorEngine(String expression, boolean degrees, double answer, double memory) {
    text = expression.replace('×', '*').replace('÷', '/')
        .replace('−', '-').replace(',', '.').replace(" ", "");
    this.degrees = degrees;
    this.answer = answer;
    this.memory = memory;
  }

  public static String calculate(String expression) {
    return calculate(expression, true, 0, 0);
  }

  public static String calculate(String expression, boolean degrees, double answer, double memory) {
    if (expression == null || expression.isBlank() || expression.length() > 256
        || !Double.isFinite(answer) || !Double.isFinite(memory))
      throw new IllegalArgumentException("Invalid expression");
    CalculatorEngine parser = new CalculatorEngine(expression, degrees, answer, memory);
    double value = parser.sum();
    if (parser.position != parser.text.length()) throw new IllegalArgumentException("Unexpected input");
    return format(value);
  }

  public static String format(double value) {
    if (!Double.isFinite(value)) throw new IllegalArgumentException("Invalid result");
    return value == 0 ? "0"
        : BigDecimal.valueOf(value).round(new MathContext(12)).stripTrailingZeros().toPlainString();
  }

  private boolean take(char c) {
    if (position < text.length() && text.charAt(position) == c) {
      position++;
      return true;
    }
    return false;
  }

  private boolean take(String token) {
    if (text.startsWith(token, position)) {
      position += token.length();
      return true;
    }
    return false;
  }

  private double sum() {
    double value = product();
    while (true) {
      if (take('+')) value += product();
      else if (take('-')) value -= product();
      else return value;
    }
  }

  private double product() {
    double value = unary();
    while (true) {
      if (take('*')) value *= unary();
      else if (take('/')) {
        double divisor = unary();
        if (divisor == 0) throw new IllegalArgumentException("Division by zero");
        value /= divisor;
      } else if (take("nPr")) value = permutations(value, unary());
      else if (take("nCr")) value = combinations(value, unary());
      else if (startsAtom()) value *= unary();
      else return value;
    }
  }

  private boolean startsAtom() {
    if (position >= text.length()) return false;
    char c = text.charAt(position);
    return c == '(' || c == 'π' || c == '.' || Character.isDigit(c)
        || Character.isLetter(c) && !text.startsWith("nPr", position)
            && !text.startsWith("nCr", position);
  }

  private double unary() {
    if (take('+')) return unary();
    if (take('-')) return -unary();
    return power();
  }

  private double power() {
    double value = postfix();
    if (take('^')) value = Math.pow(value, unary());
    return value;
  }

  private double postfix() {
    double value = atom();
    while (true) {
      if (take('%')) value /= 100;
      else if (take('!')) value = factorial(value);
      else return value;
    }
  }

  private double atom() {
    if (take('(')) {
      double value = sum();
      if (!take(')')) throw new IllegalArgumentException("Missing parenthesis");
      return value;
    }
    if (take('π')) return Math.PI;
    if (take("Ans")) return answer;
    if (take('M')) return memory;
    for (String name : new String[] {"asin", "acos", "atan", "sin", "cos", "tan", "sqrt", "cbrt", "log", "ln", "abs", "exp"}) {
      if (take(name)) {
        if (!take('(')) throw new IllegalArgumentException("Missing function argument");
        double input = sum();
        if (!take(')')) throw new IllegalArgumentException("Missing parenthesis");
        return function(name, input);
      }
    }
    if (take('e')) return Math.E;
    int begin = position;
    while (position < text.length()
        && (Character.isDigit(text.charAt(position)) || text.charAt(position) == '.')) position++;
    if (begin == position) throw new IllegalArgumentException("Missing number");
    try { return Double.parseDouble(text.substring(begin, position)); }
    catch (NumberFormatException error) { throw new IllegalArgumentException("Invalid number", error); }
  }

  private double function(String name, double x) {
    double radians = degrees ? Math.toRadians(x) : x;
    switch (name) {
      case "sin": return nearZero(Math.sin(radians));
      case "cos": return nearZero(Math.cos(radians));
      case "tan":
        if (Math.abs(Math.cos(radians)) < 1e-12) throw new IllegalArgumentException("Undefined tangent");
        return nearZero(Math.tan(radians));
      case "asin": return degrees ? Math.toDegrees(Math.asin(x)) : Math.asin(x);
      case "acos": return degrees ? Math.toDegrees(Math.acos(x)) : Math.acos(x);
      case "atan": return degrees ? Math.toDegrees(Math.atan(x)) : Math.atan(x);
      case "sqrt": if (x < 0) throw new IllegalArgumentException("Invalid root"); return Math.sqrt(x);
      case "cbrt": return Math.cbrt(x);
      case "log": if (x <= 0) throw new IllegalArgumentException("Invalid log"); return Math.log10(x);
      case "ln": if (x <= 0) throw new IllegalArgumentException("Invalid log"); return Math.log(x);
      case "abs": return Math.abs(x);
      case "exp": return Math.exp(x);
      default: throw new IllegalArgumentException("Unknown function");
    }
  }

  private static double nearZero(double value) {
    return Math.abs(value) < 1e-12 ? 0 : value;
  }

  private static int whole(double value) {
    if (!Double.isFinite(value) || value < 0 || value > 170 || value != Math.floor(value))
      throw new IllegalArgumentException("Expected whole number from 0 to 170");
    return (int) value;
  }

  private static double factorial(double value) {
    int n = whole(value);
    double result = 1;
    for (int i = 2; i <= n; i++) result *= i;
    return result;
  }

  private static double permutations(double left, double right) {
    int n = whole(left), r = whole(right);
    if (r > n) throw new IllegalArgumentException("Invalid permutation");
    double result = 1;
    for (int i = n - r + 1; i <= n; i++) result *= i;
    return result;
  }

  private static double combinations(double left, double right) {
    int n = whole(left), r = whole(right);
    if (r > n) throw new IllegalArgumentException("Invalid combination");
    r = Math.min(r, n - r);
    double result = 1;
    for (int i = 1; i <= r; i++) result *= (double) (n - r + i) / i;
    return Math.rint(result);
  }
}
