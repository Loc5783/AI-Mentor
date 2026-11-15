package com.example.mathmodule;

import static org.junit.Assert.*;

import com.example.mathmodule.calculator.CalculatorEngine;
import org.junit.Test;

public class CalculatorRegressionTest {
  @Test
  public void standardOperatorPrecedence() {
    assertEquals("14", CalculatorEngine.calculate("2+3×4"));
    assertEquals("20", CalculatorEngine.calculate("(2+3)×4"));
  }

  @Test
  public void decimalAndLocalizedComma() {
    assertEquals("0.3", CalculatorEngine.calculate("0.1+0.2"));
    assertEquals("3.75", CalculatorEngine.calculate("1,5+2,25"));
  }

  @Test
  public void powersAssociateToTheRight() {
    assertEquals("512", CalculatorEngine.calculate("2^3^2"));
    assertEquals("-4", CalculatorEngine.calculate("-2^2"));
  }

  @Test
  public void percentIsExplicitlyPostfix() {
    assertEquals("0.5", CalculatorEngine.calculate("50%"));
    assertEquals("100", CalculatorEngine.calculate("200×50%"));
  }

  @Test
  public void rootsAndNegativeNumbers() {
    assertEquals("7", CalculatorEngine.calculate("sqrt(9)+4"));
    assertEquals("2", CalculatorEngine.calculate("-3+5"));
  }

  @Test
  public void scientificFunctionsAndAngleModes() {
    assertEquals("1", CalculatorEngine.calculate("sin(90)"));
    assertEquals("0", CalculatorEngine.calculate("cos(90)"));
    assertEquals("0.5", CalculatorEngine.calculate("sin(30)"));
    assertEquals("90", CalculatorEngine.calculate("asin(1)"));
    assertEquals("1", CalculatorEngine.calculate("sin(π/2)", false, 0, 0));
    assertEquals("2", CalculatorEngine.calculate("log(100)+ln(e)-1"));
    assertEquals("-2", CalculatorEngine.calculate("cbrt(-8)"));
  }

  @Test
  public void combinationsMemoryAndPreviousAnswer() {
    assertEquals("10", CalculatorEngine.calculate("5nCr2"));
    assertEquals("20", CalculatorEngine.calculate("5nPr2"));
    assertEquals("120", CalculatorEngine.calculate("5!"));
    assertEquals("24", CalculatorEngine.calculate("Ans×M", true, 6, 4));
    assertEquals("6.28318530718", CalculatorEngine.calculate("2π"));
    assertEquals("14", CalculatorEngine.calculate("2(3+4)"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void tangentAtRightAngleIsRejected() {
    CalculatorEngine.calculate("tan(90)");
  }

  @Test(expected = IllegalArgumentException.class)
  public void divisionByZeroIsRejected() {
    CalculatorEngine.calculate("1÷0");
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeRootIsRejected() {
    CalculatorEngine.calculate("sqrt(-1)");
  }

  @Test(expected = IllegalArgumentException.class)
  public void incompleteExpressionIsRejected() {
    CalculatorEngine.calculate("2+");
  }

  @Test(expected = IllegalArgumentException.class)
  public void overflowIsRejected() {
    CalculatorEngine.calculate("10^1000");
  }
}
