package com.nikodoko.javaimports.stdlib;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import com.nikodoko.javaimports.parser.Import;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BasicStdlibProviderTest {
  BasicStdlibProvider stdlib = new BasicStdlibProvider(new FakeStdlib());

  @Test
  void testFindImportWithNoDuplicates() {
    Map<String, Import> got = stdlib.find(ImmutableSet.of("Component"));
    Import expected = new Import("Component", "java.awt", false);

    assertThat(got.get("Component")).isEqualTo(expected);
  }

  @Test
  void testFindPrioritizesShortPath() {
    Map<String, Import> got = stdlib.find(ImmutableSet.of("Duration"));
    Import expected = new Import("Duration", "java.time", false);

    assertThat(got.get("Duration")).isEqualTo(expected);
  }

  @Test
  void testFindPrioritizesJavaUtilIfConflict() {
    Map<String, Import> got = stdlib.find(ImmutableSet.of("List"));
    Import expected = new Import("List", "java.util", false);

    assertThat(got.get("List")).isEqualTo(expected);
  }

  @Test
  void testFindPrioritizesPackagesAlreadyInUse() {
    Map<String, Import> got = stdlib.find(ImmutableSet.of("List", "Component"));
    Import expected = new Import("List", "java.awt", false);

    assertThat(got.get("List")).isEqualTo(expected);
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
