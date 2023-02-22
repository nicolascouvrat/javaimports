package com.nikodoko.javaimports.stdlib;

import static com.google.common.truth.Truth.assertThat;
import static com.nikodoko.javaimports.common.CommonTestUtil.anImport;

import com.nikodoko.javaimports.common.Identifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class BasicStdlibProviderTest {
  BasicStdlibProvider stdlib = new BasicStdlibProvider(new FakeStdlib());

  @Test
  void testFindImports() {
    var expected = List.of(anImport("java.time.Duration"), anImport("javax.xml.datatype.Duration"));

    var got = stdlib.findImports(new Identifier("Duration"));

    assertThat(got).containsExactlyElementsIn(expected);
  }

  @Test
  void testIndentifierIsInJavaLang() {
    assertThat(stdlib.isInJavaLang("Object")).isTrue();
  }

  @Test
  void testNonExistingIdentifierIsNotInJavaLang() {
    assertThat(stdlib.isInJavaLang("Derp")).isFalse();
  }

  @Test
  void testIdentifierIsNotInJavaLang() {
    assertThat(stdlib.isInJavaLang("List")).isFalse();
  }

  @Test
  void testIdentifierInSubScopeOfJavaLangIsNotConsideredInJavaLang() {
    assertThat(stdlib.isInJavaLang("State")).isFalse();
  }
}
