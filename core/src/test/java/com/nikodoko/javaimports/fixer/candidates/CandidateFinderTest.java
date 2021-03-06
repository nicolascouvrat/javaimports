package com.nikodoko.javaimports.fixer.candidates;

import static com.google.common.truth.Truth.assertThat;

import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.ImportProvider;
import com.nikodoko.javaimports.common.Selector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

public class CandidateFinderTest {
  @Property
  boolean theDefaultCandidateFinderReturnsEmptyCandidates(@ForAll Selector aSelector) {
    return new CandidateFinder().find(aSelector).isEmpty();
  }

  @Example
  void itShouldReturnImportsMatchingASelectorOfLengthEqualToOne() {
    var finder = new CandidateFinder();
    finder.add(
        Candidate.Source.STDLIB,
        providerOf("static com.myapp.MyClass.CONSTANT", "com.myapp.MyClass"));

    assertThat(finder.find(Selector.of("MyClass")))
        .isEqualTo(
            Candidates.forSelector(Selector.of("MyClass"))
                .add(new Candidate(importOf("com.myapp.MyClass"), Candidate.Source.STDLIB))
                .build());
  }

  @Example
  void itShouldReturnImportsMatchingASelectorOfLengthSuperiorToOne() {
    var finder = new CandidateFinder();
    finder.add(
        Candidate.Source.STDLIB, providerOf("com.myapp.MyClass", "com.myapp.MyClass.Subclass"));

    assertThat(finder.find(Selector.of("MyClass", "Subclass")))
        .isEqualTo(
            Candidates.forSelector(Selector.of("MyClass", "Subclass"))
                .add(new Candidate(importOf("com.myapp.MyClass"), Candidate.Source.STDLIB))
                .build());
  }

  static ImportProvider providerOf(String... importStatements) {
    var importsByIdentifier = new HashMap<Identifier, List<Import>>();
    for (String statement : importStatements) {
      var i = importOf(statement);
      var imports =
          importsByIdentifier.computeIfAbsent(i.selector.identifier(), __ -> new ArrayList<>());
      imports.add(i);
    }

    return ident -> importsByIdentifier.getOrDefault(ident, new ArrayList<>());
  }

  static Import importOf(String statement) {
    var STATIC = "static ";
    var isStatic = statement.startsWith(STATIC);
    var trimmed = isStatic ? statement.substring(STATIC.length()) : statement;
    var fragments = trimmed.split("\\.");
    return new Import(
        Selector.of(fragments[0], Arrays.copyOfRange(fragments, 1, fragments.length)), isStatic);
  }
}
