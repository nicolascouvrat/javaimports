package com.nikodoko.javaimports.common;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
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

  @Property
  void subtractLeavesOriginalSelectorsUnchanged(
      @ForAll("identifiers") List<String> identifiers, @ForAll @NotEmpty String additional) {
    identifiers.add(additional);
    var tail = identifiers.stream().skip(identifiers.size() / 2).collect(Collectors.toList());
    var identifiersCopy = List.copyOf(identifiers);
    var tailCopy = List.copyOf(tail);
    var aSelector = Selector.of(identifiers);
    var aSelectorTail = Selector.of(tail);

    aSelector.subtract(aSelectorTail);

    assertThat(aSelector).isEqualTo(Selector.of(identifiersCopy));
    assertThat(aSelectorTail).isEqualTo(Selector.of(tailCopy));
  }

  @Example
  void joinDoesNotDuplicateCommonIdentifier() {
    var head = Selector.of("a", "b", "c");
    var tail = Selector.of("c", "d", "e");
    var selector = Selector.of("a", "b", "c", "d", "e");
    assertThat(head.join(tail)).isEqualTo(selector);
  }

  @Example
  void subtractLeavesLastCommonIdentifierInResult() {
    var selector = Selector.of("a", "b", "c", "d", "e");
    var tail = Selector.of("c", "d", "e");
    var head = Selector.of("a", "b", "c");
    assertThat(selector.subtract(tail)).isEqualTo(head);
  }

  @Property
  void subtractIsOppositeOfJoin(
      @ForAll("identifiers") List<String> identifiers, @ForAll @NotEmpty String additional) {
    identifiers.add(additional);
    var tail =
        Selector.of(identifiers.stream().skip(identifiers.size() / 2).collect(Collectors.toList()));
    var selector = Selector.of(identifiers);

    assertThat(selector.subtract(tail).join(tail)).isEqualTo(selector);
  }

  @Provide
  Arbitrary<List<String>> identifiers() {
    return CommonTestUtil.arbitraryIdentifiers();
  }
}
