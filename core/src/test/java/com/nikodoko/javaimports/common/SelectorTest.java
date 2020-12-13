package com.nikodoko.javaimports.common;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.NotEmpty;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.Unique;

public class SelectorTest {
  @Property
  void theSelectorConstructorCopiesArguments(
      @ForAll @Size(min = 1, max = 8) List<@Unique @NotEmpty String> identifiers) {
    var original = List.copyOf(identifiers);
    var selector2 =
        Selector.of(identifiers.get(0), identifiers.stream().skip(1).toArray(String[]::new));
    var selector1 = Selector.of(identifiers);
    identifiers.set(0, "");
    assertThat(selector1).isEqualTo(Selector.of(original));
    assertThat(selector2).isEqualTo(Selector.of(original));
  }
  // by nikodoko.com
}
