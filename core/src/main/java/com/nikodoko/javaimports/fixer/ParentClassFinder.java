package com.nikodoko.javaimports.fixer;

import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.common.ClassDeclaration;
import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.OrphanClass;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Superclass;
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
 * OrphanClass}.
 */
class ParentClassFinder {
  static class Result {
    // Whether all parents were found
    final boolean complete;
    // All identifiers that are not declared in parents
    final Set<Identifier> unresolved;
    // All imports required to access the parents found
    final Set<Import> fixes;

    Result(boolean complete, Set<Identifier> unresolved, Set<Import> fixes) {
      this.complete = complete;
      this.unresolved = unresolved;
      this.fixes = fixes;
    }

    static Result complete(Set<Identifier> unresolved, Set<Import> fixes) {
      return new Result(true, unresolved, fixes);
    }

    static Result incomplete(Set<Identifier> unresolved, Set<Import> fixes) {
      return new Result(false, unresolved, fixes);
    }

    static Result merge(Result a, Result b) {
      var complete = a.complete && b.complete;
      var unresolved = new HashSet<>(a.unresolved);
      unresolved.addAll(b.unresolved);
      var fixes = new HashSet<>(a.fixes);
      fixes.addAll(b.fixes);
      return new Result(complete, unresolved, fixes);
    }
  }

  private static Logger log = Logger.getLogger(ParentClassFinder.class.getName());
  private final CandidateFinder candidateFinder;
  private final ClassLibrary library;
  private final CandidateSelectionStrategy selectionStrategy;
  private final Options options;

  ParentClassFinder(
      CandidateFinder candidateFinder,
      ClassLibrary library,
      CandidateSelectionStrategy selectionStrategy,
      Options options) {
    this.candidateFinder = candidateFinder;
    this.library = library;
    this.selectionStrategy = selectionStrategy;
    this.options = options;
  }

  Result findAllParents(Set<OrphanClass> orphans) {
    var span = Traces.createSpan("ParentClassFinder.findAllParents");
    try (var __ = Traces.activate(span)) {
      return findAllParents(Orphans.wrapping(orphans));
    } finally {
      span.finish();
    }
  }

  Result findAllParents(Orphans orphans) {
    Set<Import> fixes = new HashSet<>();
    // Hacky, but that's to emulate the fact that we only need to import the first class in the
    // extend chain. This should be handled by the fact that extension of files should all be
    // resolved.
    Set<Selector> processed = new HashSet<>();
    while (traverseOnce(orphans, fixes, processed) > 0) {}

    if (orphans.needsParents()) {
      return Result.incomplete(orphans.unresolved(), fixes);
    }

    return Result.complete(orphans.unresolved(), fixes);
  }

  int traverseOnce(Orphans orphans, Set<Import> fixes, Set<Selector> processed) {
    var it = orphans.traverse();
    ClassDeclaration cd;
    int foundCount = 0;
    while ((cd = it.next()) != null) {
      if (cd.maybeParent().isEmpty()) {
        it.addParent(null);
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
        it.addParent(null);
        continue;
      }

      it.addParent(maybeParent.get());
      foundCount++;
    }

    return foundCount;
  }

  private Optional<ClassEntity> maybeParentClass(Superclass parent) {
    Optional<Import> maybeParent = Optional.empty();
    if (parent.isResolved()) {
      maybeParent = Optional.of(parent.getResolved());
    } else {
      maybeParent =
          maybeParentScope(parent)
              .map(s -> s.join(parent.getUnresolved()))
              // A class import is necessarily non static
              .map(s -> new Import(s, false));
    }

    var got = maybeParent.flatMap(library::find);
    if (options.debug()) {
      log.info(String.format("Found parent for %s: %s", parent, got));
    }

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
