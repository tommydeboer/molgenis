package org.molgenis.data.validation;

import static java.lang.Boolean.FALSE;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.molgenis.data.Entity;
import org.molgenis.js.magma.JsMagmaScriptEvaluator;
import org.molgenis.script.core.ScriptException;
import org.molgenis.test.AbstractMockitoTest;

class ExpressionValidatorTest extends AbstractMockitoTest {
  @Mock private JsMagmaScriptEvaluator jsMagmaScriptEvaluator;
  @Mock private Entity entity;
  private ExpressionValidator expressionValidator;

  @BeforeEach
  void beforeMethod() {
    expressionValidator = new ExpressionValidator(jsMagmaScriptEvaluator);
  }

  @Test
  void testResolveBooleanExpressions() {
    when(jsMagmaScriptEvaluator.eval("a", entity)).thenReturn(true);
    when(jsMagmaScriptEvaluator.eval("b", entity)).thenReturn(false);
    assertEquals(
        asList(true, false),
        expressionValidator.resolveBooleanExpressions(Arrays.asList("a", "b"), entity));
  }

  static Object[][] resultProvider() {
    // @formatter:off
    return new Object[][] {
      new Object[] {FALSE, false},
      new Object[] {"true", true},
      new Object[] {"TRUE", true},
      new Object[] {0, false},
      new Object[] {1, false},
      new Object[] {
        new ScriptException("Evaluation failed on line 0, column 10: Undefined is not an object"),
        false
      }
    };
    // @formatter:on
  }

  @ParameterizedTest
  @MethodSource("resultProvider")
  void testResolveBooleanExpression(Object result, boolean expected) {
    when(jsMagmaScriptEvaluator.eval("expression", entity)).thenReturn(result);
    assertEquals(expected, expressionValidator.resolveBooleanExpression("expression", entity));
  }
}
