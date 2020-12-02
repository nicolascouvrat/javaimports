package com.nikodoko.javaimports.fixer.candidates;

import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.ImportProvider;
import com.nikodoko.javaimports.common.Selector;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

public class CandidateFinderTest {
  static class DummyProvider implements ImportProvider {
    Map<Identifier, List<Import>> data =
        Map.of(
            new Identifier("MyClass"),
            List.of(new Import("com.myapp", Selector.of("MyClass"), false)),
            new Identifier("Subclass"),
            List.of(new Import("com.myapp", Selector.of("MyClass", "Subclass"), false)),
            new Identifier("CONSTANT"),
            List.of(
                new Import("com.myapp", Selector.of("MyClass", "CONSTANT"), true),
                new Import("com.myapp", Selector.of("MyClass", "Subclass", "CONSTANT"), true)));

    public Iterable<Import> findImports(Identifier identifier) {
      return Optional.ofNullable(data.get(identifier)).orElse(new ArrayList<>());
    }
  }

  @Property
  boolean theDefaultCandidateFinderReturnsEmptyCandidates(
      @ForAll("arbitrarySelector") Selector aSelector) {
    return new CandidateFinder().find(aSelector).isEmpty();
  }

  @Provide
  Arbitrary<Selector> arbitrarySelector() {
    return Arbitraries.strings().all().list().ofMinSize(1).ofMaxSize(10).map(Selector::of);
  }
}
