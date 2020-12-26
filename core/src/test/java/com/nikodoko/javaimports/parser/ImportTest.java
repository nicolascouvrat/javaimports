package com.nikodoko.javaimports.parser;

import static com.google.common.truth.Truth.assertThat;

import com.nikodoko.javaimports.common.Selector;
import org.junit.jupiter.api.Test;

// TODO: delete
public class ImportTest {
  @Test
  void testToNew() {
    var oldImport = new Import("Test", "a.b.c", true);
    assertThat(oldImport.toNew())
        .isEqualTo(
            new com.nikodoko.javaimports.common.Import(Selector.of("a", "b", "c", "Test"), true));
  }

  @Test
  void testFromNew() {
    var newImport =
        new com.nikodoko.javaimports.common.Import(Selector.of("a", "b", "c", "Test"), true);
    assertThat(Import.fromNew(newImport)).isEqualTo(new Import("Test", "a.b.c", true));
  }
}
