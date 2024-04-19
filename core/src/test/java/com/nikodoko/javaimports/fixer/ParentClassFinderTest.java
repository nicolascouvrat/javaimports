package com.nikodoko.javaimports.fixer;

import static com.google.common.truth.Truth.assertThat;
import static com.nikodoko.javaimports.common.CommonTestUtil.anImport;

import com.nikodoko.javaimports.common.ClassDeclaration;
import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Superclass;
import com.nikodoko.javaimports.fixer.candidates.Candidate;
import com.nikodoko.javaimports.fixer.candidates.CandidateFinder;
import com.nikodoko.javaimports.fixer.candidates.TakeFirstCandidateSelectionStrategy;
import com.nikodoko.javaimports.parser.Orphans;
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
    finder = new ParentClassFinder(candidates, library, new TakeFirstCandidateSelectionStrategy());
  }

  static class SimpleOrphan implements Orphans {
    Selector name;
    Set<Identifier> unresolved;
    Optional<Superclass> parent;

    SimpleOrphan() {
      this.name = null;
      this.unresolved = Set.of();
      this.parent = Optional.empty();
    }

    SimpleOrphan(Selector name, Set<Identifier> unresolved, Superclass parent) {
      this.name = name;
      this.unresolved = unresolved;
      this.parent = Optional.of(parent);
    }

    class SimpleTraverser implements Orphans.Traverser {
      boolean called = false;

      @Override
      public ClassDeclaration next() {
        if (called) {
          return null;
        }

        called = true;
        return new ClassDeclaration(name, parent);
      }

      @Override
      public void addParent(Import parentImport, ClassEntity entity) {
        entity.declarations.forEach(unresolved::remove);
        parent = entity.maybeParent;
      }
    }

    @Override
    public boolean needsParents() {
      return parent.isPresent();
    }

    @Override
    public Orphans.Traverser traverse() {
      return new SimpleTraverser();
    }
  }

  @Test
  void itShouldReturnAnEmptyResultIfNoOrphan() {
    var got = finder.findAllParents(new SimpleOrphan());
    assertThat(got.complete).isTrue();
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
        new SimpleOrphan(
            Selector.of("Orphan"),
            identifiers("a", "b", "c"),
            Superclass.unresolved(Selector.of("FirstParent")));

    var got = finder.findAllParents(orphan);
    assertThat(got.complete).isTrue();
    assertThat(got.fixes).containsExactly(anImport("com.app.FirstParent"));
    assertThat(orphan.unresolved).containsExactlyElementsIn(identifiers("c"));
  }

  Set<Identifier> identifiers(String... identifiers) {
    return Arrays.stream(identifiers).map(Identifier::new).collect(Collectors.toSet());
  }
}
