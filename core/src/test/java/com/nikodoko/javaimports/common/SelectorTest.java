package com.nikodoko.javaimports.common;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

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
      @ForAll("identifier") String common,
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
      @ForAll("identifiers") List<String> identifiers, @ForAll("identifier") String additional) {
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
      @ForAll("identifiers") List<String> identifiers, @ForAll("identifier") String additional) {
    identifiers.add(additional);
    var tail =
        Selector.of(identifiers.stream().skip(identifiers.size() / 2).collect(Collectors.toList()));
    var selector = Selector.of(identifiers);

    assertThat(selector.subtract(tail).join(tail)).isEqualTo(selector);
  }

  @Property
  void distanceToSameSelectorIsZero(@ForAll Selector aSelector) {
    var distance = Selector.Distance.from(aSelector);
    var got = distance.to(aSelector);

    assertThat(got).isEqualTo(0);
  }

  @Property
  void distanceToChildSelectorIsEqualToSizeDelta(
      @ForAll Selector aSelector, @ForAll Selector anotherSelector) {
    var childSelector = aSelector.combine(anotherSelector);
    var distance = Selector.Distance.from(aSelector);
    var got = distance.to(childSelector);

    assertThat(got).isEqualTo(childSelector.size() - aSelector.size());
  }

  @Property
  void distanceToScopeIsOne(@ForAll("selectorWithScope") Selector aSelector) {
    var distance = Selector.Distance.from(aSelector);
    var got = distance.to(aSelector.scope());

    assertThat(got).isEqualTo(1);
  }

  @Property
  void distanceToSelectorInTheSameScopeIsTwo(
      @ForAll("selectorWithScope") Selector aSelector, @ForAll("identifier") String additional) {
    var sibling = aSelector.scope().combine(Selector.of(additional));
    var distance = Selector.Distance.from(aSelector);
    var got = distance.to(sibling);

    assertThat(got).isEqualTo(sibling.equals(aSelector) ? 0 : 2);
  }

  @Example
  void distanceToSelectorThatDoesNotShareACommonRootIsEqualToSumOfSizes(
      @ForAll Selector aSelector, @ForAll Selector anotherSelector) {
    var a = Selector.of("a").combine(aSelector);
    var b = Selector.of("b").combine(anotherSelector);
    var distance = Selector.Distance.from(a);
    var got = distance.to(b);

    assertThat(got).isEqualTo(aSelector.size() + anotherSelector.size() + 2);
  }

  @Provide
  Arbitrary<List<String>> identifiers() {
    return CommonTestUtil.arbitraryIdentifiersOfSize(1, 8);
  }

  @Provide
  Arbitrary<String> identifier() {
    return CommonTestUtil.arbitraryIdentifier();
  }

  @Provide
  Arbitrary<Selector> selectorWithScope() {
    return CommonTestUtil.arbitrarySelectorOfSize(2, 8);
  }
}
