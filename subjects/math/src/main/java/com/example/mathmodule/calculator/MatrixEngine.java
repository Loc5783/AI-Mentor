package com.example.mathmodule.calculator;

/** Small square matrices used by the calculator's matrix mode. */
public final class MatrixEngine {
  private MatrixEngine() {}

  public static double[][] checked(double[][] matrix) {
    if (matrix == null || matrix.length < 2 || matrix.length > 3)
      throw new IllegalArgumentException("Only 2×2 and 3×3 matrices are supported");
    for (double[] row : matrix) {
      if (row == null || row.length != matrix.length) throw new IllegalArgumentException("Matrix must be square");
      for (double value : row) if (!Double.isFinite(value)) throw new IllegalArgumentException("Invalid matrix value");
    }
    return matrix;
  }

  private static void sameSize(double[][] a, double[][] b) {
    checked(a); checked(b);
    if (a.length != b.length) throw new IllegalArgumentException("Matrix sizes differ");
  }

  public static double[][] add(double[][] a, double[][] b) {
    sameSize(a, b);
    double[][] result = new double[a.length][a.length];
    for (int r = 0; r < a.length; r++) for (int c = 0; c < a.length; c++) result[r][c] = a[r][c] + b[r][c];
    return checked(result);
  }

  public static double[][] subtract(double[][] a, double[][] b) {
    sameSize(a, b);
    double[][] result = new double[a.length][a.length];
    for (int r = 0; r < a.length; r++) for (int c = 0; c < a.length; c++) result[r][c] = a[r][c] - b[r][c];
    return checked(result);
  }

  public static double[][] multiply(double[][] a, double[][] b) {
    sameSize(a, b);
    double[][] result = new double[a.length][a.length];
    for (int r = 0; r < a.length; r++) for (int c = 0; c < a.length; c++)
      for (int k = 0; k < a.length; k++) result[r][c] += a[r][k] * b[k][c];
    return checked(result);
  }

  public static double[][] transpose(double[][] a) {
    checked(a);
    double[][] result = new double[a.length][a.length];
    for (int r = 0; r < a.length; r++) for (int c = 0; c < a.length; c++) result[c][r] = a[r][c];
    return result;
  }

  public static double determinant(double[][] a) {
    checked(a);
    double[][] work = copy(a);
    double tolerance = tolerance(a);
    double result = 1;
    for (int column = 0; column < work.length; column++) {
      int pivot = pivotRow(work, column);
      if (Math.abs(work[pivot][column]) <= tolerance) return 0;
      if (pivot != column) {
        double[] swap = work[pivot]; work[pivot] = work[column]; work[column] = swap;
        result = -result;
      }
      double diagonal = work[column][column];
      result *= diagonal;
      for (int row = column + 1; row < work.length; row++) {
        double factor = work[row][column] / diagonal;
        for (int col = column + 1; col < work.length; col++) work[row][col] -= factor * work[column][col];
      }
    }
    return result;
  }

  public static double[][] inverse(double[][] a) {
    checked(a);
    int n = a.length;
    double tolerance = tolerance(a);
    double[][] work = new double[n][2 * n];
    for (int row = 0; row < n; row++) {
      System.arraycopy(a[row], 0, work[row], 0, n);
      work[row][n + row] = 1;
    }
    for (int col = 0; col < n; col++) {
      int pivot = pivotRow(work, col);
      if (Math.abs(work[pivot][col]) <= tolerance) throw new IllegalArgumentException("Matrix has no inverse");
      double[] swap = work[pivot]; work[pivot] = work[col]; work[col] = swap;
      double divisor = work[col][col];
      for (int j = 0; j < 2 * n; j++) work[col][j] /= divisor;
      for (int row = 0; row < n; row++) {
        if (row == col) continue;
        double factor = work[row][col];
        for (int j = 0; j < 2 * n; j++) work[row][j] -= factor * work[col][j];
      }
    }
    double[][] result = new double[n][n];
    for (int row = 0; row < n; row++) System.arraycopy(work[row], n, result[row], 0, n);
    return checked(result);
  }

  private static int pivotRow(double[][] matrix, int column) {
    int pivot = column;
    for (int row = column + 1; row < matrix.length; row++)
      if (Math.abs(matrix[row][column]) > Math.abs(matrix[pivot][column])) pivot = row;
    return pivot;
  }

  private static double tolerance(double[][] matrix) {
    double scale = 0;
    for (double[] row : matrix) for (double value : row) scale = Math.max(scale, Math.abs(value));
    return scale * 1e-14;
  }

  private static double[][] copy(double[][] matrix) {
    double[][] result = new double[matrix.length][];
    for (int row = 0; row < matrix.length; row++) result[row] = matrix[row].clone();
    return result;
  }

  public static String format(double[][] matrix) {
    checked(matrix);
    StringBuilder result = new StringBuilder();
    for (double[] row : matrix) {
      if (result.length() > 0) result.append('\n');
      result.append("[ ");
      for (int col = 0; col < row.length; col++) {
        if (col > 0) result.append("    ");
        result.append(CalculatorEngine.format(row[col]));
      }
      result.append(" ]");
    }
    return result.toString();
  }
}
