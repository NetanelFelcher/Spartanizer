package il.org.spartan.spartanizer.wrings;

import static il.org.spartan.spartanizer.wrings.TrimmerTestsUtils.*;

import org.junit.*;
import org.junit.runners.*;

/** Unit tests for {@link NameYourClassHere}
 * @author Niv Shalmon
 * @since 2016 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING) //
@SuppressWarnings({ "static-method", "javadoc" }) //
public class Issue116Test {
  @Test public void issue116_01() {
    trimming("\"\" + x").to("x + \"\"").stays();
  }

  @Test public void issue116_02() {
    trimming("\"\" + x.foo()").to("x.foo() + \"\"").stays();
  }

  @Test public void issue116_03() {
    trimming("\"\" + (Integer)(\"\" + x).length()").to("(Integer)(\"\" + x).length() + \"\"").to("(Integer)(x +\"\").length() + \"\"").stays();
  }

  @Test public void issue116_04() {
    trimming("String s = \"\" + x.foo();").to("String s = x.foo() + \"\";").stays();
  }

  @Test public void issue116_07() {
    trimming("\"\" + 0 + (x - 7)").to("0 + \"\" + (x - 7)").stays();
  }

  @Test public void issue116_08() {
    trimming("return x == null ? \"Use isEmpty()\" : \"Use \" + x + \".isEmpty()\";")
        .to("return \"Use \" + (x == null ? \"isEmpty()\" : \"\" + x + \".isEmpty()\");")
        .to("return \"Use \" + ((x == null ? \"\" : \"\" + x + \".\")+\"isEmpty()\");")
        .to("return \"Use \" + (x == null ? \"\" : \"\" + x  + \".\")+\"isEmpty()\";")
        .to("return \"Use \" + (x == null ? \"\" : x +\"\" + \".\")+\"isEmpty()\";")
        .to("return \"Use \" + (x == null ? \"\" : x + \".\")+\"isEmpty()\";").stays();
  }
}