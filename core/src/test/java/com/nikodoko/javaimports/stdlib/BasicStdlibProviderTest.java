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
}
