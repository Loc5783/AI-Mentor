package com.example.mathmodule.calculator;

import java.util.Arrays;

/** Descriptive statistics and straight-line regression for classroom-sized datasets. */
public final class StatisticsEngine {
  private StatisticsEngine() {}

  public static final class Summary {
    public final int count;
    public final double sum, mean, median, minimum, maximum, populationVariance;
    public final double sampleVariance;

    private Summary(int count, double sum, double mean, double median, double minimum,
        double maximum, double populationVariance, double sampleVariance) {
      this.count = count;
      this.sum = sum;
      this.mean = mean;
      this.median = median;
      this.minimum = minimum;
      this.maximum = maximum;
      this.populationVariance = populationVariance;
      this.sampleVariance = sampleVariance;
    }

    public double populationStandardDeviation() { return Math.sqrt(populationVariance); }
    public double sampleStandardDeviation() { return Math.sqrt(sampleVariance); }
  }

  public static final class Regression {
    public final int count;
    public final double slope, intercept, correlation;

    private Regression(int count, double slope, double intercept, double correlation) {
      this.count = count;
      this.slope = slope;
      this.intercept = intercept;
      this.correlation = correlation;
    }
  }

  public static double[] parseValues(String input) {
    if (input == null || input.trim().isEmpty()) throw new IllegalArgumentException("Enter values");
    String[] tokens = input.trim().split("[,;\\s]+");
    if (tokens.length > 100) throw new IllegalArgumentException("Too many values");
    double[] values = new double[tokens.length];
    for (int i = 0; i < tokens.length; i++) values[i] = parseNumber(tokens[i]);
    return values;
  }

  public static double[][] parsePairs(String input) {
    if (input == null || input.trim().isEmpty()) throw new IllegalArgumentException("Enter pairs");
    String[] lines = input.trim().split("[;\\r\\n]+");
    if (lines.length < 2 || lines.length > 100) throw new IllegalArgumentException("Enter 2–100 pairs");
    double[][] pairs = new double[lines.length][2];
    for (int i = 0; i < lines.length; i++) {
      String[] parts = lines[i].trim().split("[,\\s]+");
      if (parts.length != 2) throw new IllegalArgumentException("Each row needs x,y");
      pairs[i][0] = parseNumber(parts[0]);
      pairs[i][1] = parseNumber(parts[1]);
    }
    return pairs;
  }

  private static double parseNumber(String text) {
    try {
      double value = Double.parseDouble(text);
      if (Double.isFinite(value)) return value;
    } catch (NumberFormatException ignored) { }
    throw new IllegalArgumentException("Invalid number");
  }

  public static Summary summarize(double[] values) {
    if (values == null || values.length == 0 || values.length > 100)
      throw new IllegalArgumentException("Enter 1–100 values");
    double[] sorted = values.clone();
    for (double value : sorted) if (!Double.isFinite(value)) throw new IllegalArgumentException("Invalid number");
    Arrays.sort(sorted);
    double sum = 0;
    for (double value : sorted) sum += value;
    double mean = sum / sorted.length;
    double squares = 0;
    for (double value : sorted) squares += (value - mean) * (value - mean);
    double median = sorted.length % 2 == 1 ? sorted[sorted.length / 2]
        : (sorted[sorted.length / 2 - 1] / 2 + sorted[sorted.length / 2] / 2);
    return new Summary(sorted.length, sum, mean, median, sorted[0], sorted[sorted.length - 1],
        squares / sorted.length, sorted.length > 1 ? squares / (sorted.length - 1) : Double.NaN);
  }

  public static Regression regress(double[][] pairs) {
    if (pairs == null || pairs.length < 2 || pairs.length > 100)
      throw new IllegalArgumentException("Enter 2–100 pairs");
    double sx = 0, sy = 0;
    for (double[] pair : pairs) {
      if (pair == null || pair.length != 2 || !Double.isFinite(pair[0]) || !Double.isFinite(pair[1]))
        throw new IllegalArgumentException("Invalid pair");
      sx += pair[0]; sy += pair[1];
    }
    double mx = sx / pairs.length, my = sy / pairs.length;
    double xx = 0, yy = 0, xy = 0;
    for (double[] pair : pairs) {
      double dx = pair[0] - mx, dy = pair[1] - my;
      xx += dx * dx; yy += dy * dy; xy += dx * dy;
    }
    if (xx == 0) throw new IllegalArgumentException("X values must differ");
    double slope = xy / xx;
    double intercept = my - slope * mx;
    double correlation = yy == 0 ? Double.NaN : xy / Math.sqrt(xx * yy);
    if (!Double.isFinite(slope) || !Double.isFinite(intercept))
      throw new IllegalArgumentException("Result overflow");
    return new Regression(pairs.length, slope, intercept, correlation);
  }
}
