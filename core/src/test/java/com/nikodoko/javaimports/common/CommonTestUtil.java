package com.nikodoko.javaimports.common;

import java.util.Arrays;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;

/** Test utilities for classes in the {@code common} package. */
public class CommonTestUtil {
  // This is not exactly the definition of a Java identifier but close enough
  public static Arbitrary<String> arbitraryIdentifier() {
    return Arbitraries.strings().ofMinLength(1).alpha().numeric().withChars('_', '$');
  }

  public static Arbitrary<List<String>> arbitraryIdentifiersOfSize(int minSize, int maxSize) {
    return arbitraryIdentifier().list().ofMinSize(minSize).ofMaxSize(maxSize);
  }

  public static Arbitrary<Selector> arbitrarySelector() {
    return arbitrarySelectorOfSize(1, 8);
  }

  public static Arbitrary<Selector> arbitrarySelectorOfSize(int minSize, int maxSize) {
    return arbitraryIdentifiersOfSize(minSize, maxSize).map(Selector::of);
  }

  public static Arbitrary<Import> arbitraryImportEndingWith(Selector tail) {
    var selector = arbitrarySelector();
    var isStatic = Arbitraries.of(true, false);
    return Combinators.combine(selector, isStatic).as((s, i) -> new Import(s.combine(tail), i));
  }

  public static Arbitrary<Import> arbitraryImport() {
    var selector = arbitrarySelector();
    var isStatic = Arbitraries.of(true, false);
    return Combinators.combine(selector, isStatic).as(Import::new);
  }

  public static Import anImport(String dotSelector) {
    var identifiers = dotSelector.split("\\.");
    return new Import(Selector.of(Arrays.asList(identifiers)), false);
  }
}
