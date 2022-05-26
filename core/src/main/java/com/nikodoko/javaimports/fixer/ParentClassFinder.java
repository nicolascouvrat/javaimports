package com.nikodoko.javaimports.fixer;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.OrphanClass;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.fixer.candidates.CandidateFinder;
import com.nikodoko.javaimports.fixer.candidates.CandidateSelectionStrategy;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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

  Result findAllParents(Set<OrphanClass> orphans) {
    var span = Traces.createSpan("ParentClassFinder.findAllParents");
    try (var __ = Traces.activate(span)) {
      return orphans.stream()
          .map(this::findParents)
          .reduce(Result::merge)
          .orElse(Result.complete(Set.of(), Set.of()));
    } finally {
      span.finish();
    }
  }

  Result findParents(OrphanClass orphan) {
    if (!orphan.hasParent()) {
      return Result.complete(orphan.unresolved, Set.of());
    }

    Set<Import> fixes = new HashSet<>();
    // We require a fix only if the parent is not already resolved
    if (!orphan.parent().isResolved()) {
      maybeParentScope(orphan).map(s -> new Import(s, false)).map(fixes::add);
    }

    while (orphan.hasParent() && !orphan.unresolved.isEmpty()) {
      var maybeParent = maybeParentClass(orphan);
      if (maybeParent.isEmpty()) {
        return Result.incomplete(orphan.unresolved, fixes);
      }

      orphan = orphan.addParent(maybeParent.get());
    }

    return Result.complete(orphan.unresolved, fixes);
  }

  private Optional<ClassEntity> maybeParentClass(OrphanClass orphan) {
    Optional<Import> maybeParent = Optional.empty();
    if (orphan.parent().isResolved()) {
      maybeParent = Optional.of(orphan.parent().getResolved());
    } else {
      maybeParent =
          maybeParentScope(orphan)
              .map(s -> s.join(orphan.parent().getUnresolved()))
              // A class import is necessarily non static
              .map(s -> new Import(s, false));
    }

    return maybeParent.flatMap(library::find);
  }

  // This returns the scope from which the parent is reachable. In other words,
  // findParentScope(orphan).join(orphan.parent().getUnresolved()) is a selector pointing to the
  // actual parent class
  private Optional<Selector> maybeParentScope(OrphanClass orphan) {
    var parentSelector = orphan.parent().getUnresolved();
    var candidates = candidateFinder.find(parentSelector);
    return selectionStrategy
        .selectBest(candidates)
        .forSelector(parentSelector)
        .map(i -> i.selector);
  }
}
