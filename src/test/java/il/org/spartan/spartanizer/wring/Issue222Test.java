package il.org.spartan.spartanizer.wring;

import static il.org.spartan.spartanizer.wring.TrimmerTestsUtils.*;

import org.junit.*;
import org.junit.runners.*;

/** Unit tests for centification of a single parameter to a function even if it
 * defines a "$" variable
 * @author Yossi Gil
 * @since 2016 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING) //
@SuppressWarnings({ "static-method", "javadoc" }) //
public class Issue222Test {
  @Ignore @Test public void vanilla() {
    trimming(//
        "static List<Expression> operands(final InfixExpression x) {\n" //
            + "  if (x == null)\n" //
            + "    return null;\n" //
            + "  final List<Expression> $ = new ArrayList<>();\n" //
            + "  $.add(left(x));\n" //
            + "  $.add(right(x));\n" //
            + "  if (x.hasExtendedOperands())\n" //
            + "    $.addAll(step.extendedOperands(x));\n" //
            + "  return $;\n" //
            + "}\n") //
                .to("static List<Expression> operands(final InfixExpression ¢) {\n" //
                    + "  if (¢ == null)\n" //
                    + "    return null;\n" //
                    + "  final List<Expression> $ = new ArrayList<>();\n" //
                    + "  $.add(left(¢));\n" //
                    + "  $.add(right(¢));\n" //
                    + "  if (¢.hasExtendedOperands())\n" //
                    + "    $.addAll(step.extendedOperands(¢));\n" //
                    + "  return $;\n" //
                    + "}\n") //
                .stays();
  }
}