package com.example.mathmodule;

import static org.junit.Assert.*;

import com.example.mathmodule.calculator.MatrixEngine;
import com.example.mathmodule.calculator.StatisticsEngine;
import org.junit.Test;

public class CalculatorExtrasTest {
  @Test public void matrixOperationsTwoAndThreeDimensions() {
    double[][] a = {{1, 2}, {3, 4}}, b = {{2, 0}, {1, 2}};
    assertArrayEquals(new double[] {3, 2}, MatrixEngine.add(a, b)[0], 1e-10);
    assertArrayEquals(new double[] {4, 4}, MatrixEngine.multiply(a, b)[0], 1e-10);
    assertEquals(-2, MatrixEngine.determinant(a), 1e-10);
    double[][] inverse = MatrixEngine.inverse(a);
    assertArrayEquals(new double[] {1, 0}, MatrixEngine.multiply(a, inverse)[0], 1e-10);
    assertArrayEquals(new double[] {0, 1}, MatrixEngine.multiply(a, inverse)[1], 1e-10);
    double[][] three = {{1, 2, 3}, {0, 1, 4}, {5, 6, 0}};
    assertEquals(1, MatrixEngine.determinant(three), 1e-10);
    assertArrayEquals(new double[] {1, 0, 0}, MatrixEngine.multiply(three, MatrixEngine.inverse(three))[0], 1e-10);
    assertEquals(3, MatrixEngine.transpose(three)[2][0], 1e-10);
    assertEquals(1e-26, MatrixEngine.determinant(new double[][] {{1e-13, 0}, {0, 1e-13}}), 1e-36);
  }

  @Test(expected = IllegalArgumentException.class)
  public void singularMatrixHasNoInverse() {
    MatrixEngine.inverse(new double[][] {{1, 2}, {2, 4}});
  }

  @Test public void descriptiveStatisticsAndRegression() {
    StatisticsEngine.Summary s = StatisticsEngine.summarize(StatisticsEngine.parseValues("2; 4; 6; 8"));
    assertEquals(4, s.count);
    assertEquals(20, s.sum, 1e-10);
    assertEquals(5, s.mean, 1e-10);
    assertEquals(5, s.median, 1e-10);
    assertEquals(5, s.populationVariance, 1e-10);
    assertEquals(20.0 / 3, s.sampleVariance, 1e-10);
    StatisticsEngine.Regression r = StatisticsEngine.regress(StatisticsEngine.parsePairs("1,3\n2,5\n3,7"));
    assertEquals(2, r.slope, 1e-10);
    assertEquals(1, r.intercept, 1e-10);
    assertEquals(1, r.correlation, 1e-10);
  }

  @Test(expected = IllegalArgumentException.class)
  public void regressionRejectsConstantX() {
    StatisticsEngine.regress(StatisticsEngine.parsePairs("1,2\n1,3"));
  }
}
