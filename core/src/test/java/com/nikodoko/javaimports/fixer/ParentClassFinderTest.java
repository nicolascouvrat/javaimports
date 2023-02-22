package com.nikodoko.javaimports.fixer;

import static com.google.common.truth.Truth.assertThat;
import static com.nikodoko.javaimports.common.CommonTestUtil.anImport;

import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.OrphanClass;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Superclass;
import com.nikodoko.javaimports.fixer.candidates.Candidate;
import com.nikodoko.javaimports.fixer.candidates.CandidateFinder;
import com.nikodoko.javaimports.fixer.candidates.TakeFirstCandidateSelectionStrategy;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ParentClassFinderTest {
  ClassLibrary library;
  CandidateFinder candidates;
  ParentClassFinder finder;

  @BeforeEach
  void setup() {
    library = new ClassLibrary();
    candidates = new CandidateFinder();
    finder =
        new ParentClassFinder(
            candidates, library, new TakeFirstCandidateSelectionStrategy(), Options.defaults());
  }

  @Test
  void itShouldReturnAnEmptyResultIfNoOrphan() {
    var got = finder.findAllParents(Set.of());
    assertThat(got.complete).isTrue();
    assertThat(got.unresolved).isEmpty();
    assertThat(got.fixes).isEmpty();
  }

  @Test
  void itShouldFindMultipleParentsButOnlyImportTheFirstOne() {
    var firstParent =
        ClassEntity.named(Selector.of("com", "app", "FirstParent"))
            .declaring(identifiers("a"))
            .extending(Superclass.unresolved(Selector.of("SecondParent")))
            .build();
    var secondParent =
        ClassEntity.named(Selector.of("com", "app", "SecondParent"))
            .declaring(identifiers("b"))
            .build();
    var classes =
        Map.of(
            anImport("com.app.FirstParent"),
            firstParent,
            anImport("com.app.SecondParent"),
            secondParent);
    var selectors =
        Map.of(
            new Identifier("FirstParent"),
            anImport("com.app.FirstParent"),
            new Identifier("SecondParent"),
            anImport("com.app.SecondParent"));

    library.add(i -> Optional.ofNullable(classes.get(i)));
    candidates.add(
        Candidate.Source.SIBLING,
        i -> Optional.ofNullable(selectors.get(i)).map(Set::of).orElse(Set.of()));
    var orphan =
        new OrphanClass(
            Selector.of("Orphan"),
            identifiers("a", "b", "c"),
            Superclass.unresolved(Selector.of("FirstParent")));

    var got = finder.findAllParents(Set.of(orphan));
    assertThat(got.complete).isTrue();
    assertThat(got.unresolved).containsExactlyElementsIn(identifiers("c"));
    assertThat(got.fixes).containsExactly(anImport("com.app.FirstParent"));
  }

  Set<Identifier> identifiers(String... identifiers) {
    return Arrays.stream(identifiers).map(Identifier::new).collect(Collectors.toSet());
  }
}
