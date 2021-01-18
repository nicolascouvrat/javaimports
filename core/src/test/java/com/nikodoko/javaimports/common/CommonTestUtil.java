package com.nikodoko.javaimports.common;

import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;

/** Test utilities for classes in the {@code common} package. */
public class CommonTestUtil {
  public static Arbitrary<List<String>> arbitraryIdentifiers() {
    return Arbitraries.strings().ascii().ofMinLength(1).list().ofMinSize(1).ofMaxSize(8);
  }

  public static Arbitrary<Selector> arbitrarySelector() {
    return arbitraryIdentifiers().map(Selector::of);
  }

  public static Arbitrary<Import> arbitraryImportEndingWith(Selector tail) {
    var selector = arbitrarySelector();
    var isStatic = Arbitraries.of(true, false);
    return Combinators.combine(selector, isStatic).as((s, i) -> new Import(s.combine(tail), i));
  }
}
