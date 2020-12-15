package com.nikodoko.javaimports.common;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.NotEmpty;

public class SelectorTest {
  @Property
  void theSelectorConstructorCopiesArguments(@ForAll("identifiers") List<String> identifiers) {
    var original = List.copyOf(identifiers);
    var selector2 =
        Selector.of(identifiers.get(0), identifiers.stream().skip(1).toArray(String[]::new));
    var selector1 = Selector.of(identifiers);
    identifiers.set(0, "");
    assertThat(selector1).isEqualTo(Selector.of(original));
    assertThat(selector2).isEqualTo(Selector.of(original));
  }

  @Property
  void joinLeavesOriginalSelectorsUnchanged(
      @ForAll("identifiers") List<String> first,
      @ForAll @NotEmpty String common,
      @ForAll("identifiers") List<String> last) {
    first.add(common);
    last.add(0, common);
    var firstCopy = List.copyOf(first);
    var lastCopy = List.copyOf(last);
    var aSelector = Selector.of(first);
    var anotherSelector = Selector.of(last);

    aSelector.join(anotherSelector);

    assertThat(aSelector).isEqualTo(Selector.of(firstCopy));
    assertThat(anotherSelector).isEqualTo(Selector.of(lastCopy));
  }

  @Provide
  Arbitrary<List<String>> identifiers() {
    return CommonTestUtil.arbitraryIdentifiers();
  }
}
