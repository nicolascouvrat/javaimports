package com.nikodoko.javaimports.fixer;

import com.nikodoko.javaimports.common.ClassDeclaration;
import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Superclass;
import com.nikodoko.javaimports.common.telemetry.Logs;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.fixer.candidates.CandidateFinder;
import com.nikodoko.javaimports.fixer.candidates.CandidateSelectionStrategy;
import com.nikodoko.javaimports.parser.Orphans;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A {@code ParentClassFinder} uses a {@link CandidateFinder} as well as a {@link ClassLibrary} to
 * identify unresolved identifiers that are in fact declared in one of the parents of an{@link
 * Orphans}.
 */
class ParentClassFinder {
  static class Result {
    // Whether all parents were found
    final boolean complete;
    // All imports required to access the parents found
    final Set<Import> fixes;

    Result(boolean complete, Set<Import> fixes) {
      this.complete = complete;
      this.fixes = fixes;
    }

    static Result complete(Set<Import> fixes) {
      return new Result(true, fixes);
    }

    static Result incomplete(Set<Import> fixes) {
      return new Result(false, fixes);
    }

    static Result merge(Result a, Result b) {
      var complete = a.complete && b.complete;
      var fixes = new HashSet<>(a.fixes);
      fixes.addAll(b.fixes);
      return new Result(complete, fixes);
    }
  }

  private static Logger log = Logs.getLogger(ParentClassFinder.class.getName());
  private final CandidateFinder candidateFinder;
  private final ClassLibrary library;
  private final CandidateSelectionStrategy selectionStrategy;

  ParentClassFinder(
      CandidateFinder candidateFinder,
      ClassLibrary library,
      CandidateSelectionStrategy selectionStrategy) {
    this.candidateFinder = candidateFinder;
    this.library = library;
    this.selectionStrategy = selectionStrategy;
  }

  Result findAllParents(Orphans orphans) {
    var span = Traces.createSpan("ParentClassFinder.findAllParents");
    try (var __ = Traces.activate(span)) {
      return findAllParentsInstrumented(orphans);
    } finally {
      span.finish();
    }
  }

  private Result findAllParentsInstrumented(Orphans orphans) {
    Set<Import> fixes = new HashSet<>();
    // Hacky, but that's to emulate the fact that we only need to import the first class in the
    // extend chain. This should be handled by the fact that extension of files should all be
    // resolved.
    Set<Selector> processed = new HashSet<>();
    while (traverseOnce(orphans, fixes, processed) > 0) {}

    if (orphans.needsParents()) {
      return Result.incomplete(fixes);
    }

    return Result.complete(fixes);
  }

  int traverseOnce(Orphans orphans, Set<Import> fixes, Set<Selector> processed) {
    var it = orphans.traverse();
    ClassDeclaration cd;
    int foundCount = 0;
    while ((cd = it.next()) != null) {
      if (cd.maybeParent().isEmpty()) {
        continue;
      }

      // This should be the only condition we use, but instead...
      if (!cd.maybeParent().get().isResolved()) {
        var maybeParentScope = maybeParentScope(cd.maybeParent().get());
        if (maybeParentScope.isPresent() && !processed.contains(cd.name())) {
          fixes.add(new Import(maybeParentScope.get(), false));
          processed.add(cd.name());
        }
      }

      var maybeParent = maybeParentClass(cd.maybeParent().get());
      if (maybeParent.isEmpty()) {
        continue;
      }

      var parent = maybeParent.get();
      it.addParent(parent.parentImport(), parent.parent());
      foundCount++;
    }

    return foundCount;
  }

  record ParentAndImport(ClassEntity parent, Import parentImport) {}

  private Optional<ParentAndImport> maybeParentClass(Superclass parent) {
    Optional<Import> maybeParentImport = Optional.empty();
    if (parent.isResolved()) {
      maybeParentImport = Optional.of(parent.getResolved());
    } else {
      maybeParentImport =
          maybeParentScope(parent)
              .map(s -> s.join(parent.getUnresolved()))
              // A class import is necessarily non static
              .map(s -> new Import(s, false));
    }

    var got = maybeParentImport.flatMap(i -> library.find(i).map(p -> new ParentAndImport(p, i)));
    log.info(String.format("Found parent for %s: %s", parent, got));

    return got;
  }

  // This returns the scope from which the parent is reachable. In other words,
  // findParentScope(orphan).join(orphan.parent().getUnresolved()) is a selector pointing to the
  // actual parent class
  private Optional<Selector> maybeParentScope(Superclass parent) {
    var parentSelector = parent.getUnresolved();
    var candidates = candidateFinder.find(parentSelector);
    return selectionStrategy
        .selectBest(candidates)
        .forSelector(parentSelector)
        .map(i -> i.selector);
  }
}
