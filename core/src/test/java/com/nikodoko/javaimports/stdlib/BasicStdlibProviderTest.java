package com.nikodoko.javaimports.stdlib;

import static com.google.common.truth.Truth.assertThat;
import static com.nikodoko.javaimports.common.CommonTestUtil.anImport;

import com.nikodoko.javaimports.common.Identifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class BasicStdlibProviderTest {
  BasicStdlibProvider stdlib =
      new BasicStdlibProvider(
          new FakeStdlib(
              anImport("java.lang.Thread.State"),
              anImport("java.lang.Object"),
              anImport("org.omg.CORBA.Object"),
              anImport("java.awt.List"),
              anImport("java.util.List"),
              anImport("java.time.Duration"),
              anImport("javax.xml.datatype.Duration"),
              anImport("java.awt.Component")));

  @Test
  void testFindImports() {
    var expected = List.of(anImport("java.time.Duration"), anImport("javax.xml.datatype.Duration"));

    var got = stdlib.findImports(new Identifier("Duration"));

    assertThat(got).containsExactlyElementsIn(expected);
  }

  @Test
  void testIndentifierIsInJavaLang() {
    assertThat(stdlib.isInJavaLang(new Identifier("Object"))).isTrue();
  }

  @Test
  void testNonExistingIdentifierIsNotInJavaLang() {
    assertThat(stdlib.isInJavaLang(new Identifier("Derp"))).isFalse();
  }

  @Test
  void testIdentifierIsNotInJavaLang() {
    assertThat(stdlib.isInJavaLang(new Identifier("List"))).isFalse();
  }

  @Test
  void testIdentifierInSubScopeOfJavaLangIsNotConsideredInJavaLang() {
    assertThat(stdlib.isInJavaLang(new Identifier("State"))).isFalse();
  }
}
