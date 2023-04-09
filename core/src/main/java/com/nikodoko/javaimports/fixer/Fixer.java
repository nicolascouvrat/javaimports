package com.nikodoko.javaimports.fixer;

import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.telemetry.Tag;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.environment.Environment;
import com.nikodoko.javaimports.fixer.candidates.BasicCandidateSelectionStrategy;
import com.nikodoko.javaimports.fixer.candidates.Candidate;
import com.nikodoko.javaimports.fixer.candidates.CandidateFinder;
import com.nikodoko.javaimports.fixer.candidates.CandidateSelectionStrategy;
import com.nikodoko.javaimports.fixer.candidates.Candidates;
import com.nikodoko.javaimports.fixer.internal.Loader;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.stdlib.StdlibProvider;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Given a {@link ParsedFile} with unresolved identifiers and orphan child classes, use a variety of
 * information to determine which identifiers indeed need to be imported, and how to import them.
 */
public class Fixer {
  private ParsedFile file;
  private Options options;
  private final CandidateFinder candidates;
  private final ClassLibrary library;
  private final ParentClassFinder parents;
  private final CandidateSelectionStrategy strategy;
  private static Logger log = Logger.getLogger(Fixer.class.getName());

  private Loader loader;

  private Fixer(ParsedFile file, Options options) {
    this.file = file;
    this.options = options;
    this.candidates = new CandidateFinder();
    this.library = new ClassLibrary();
    this.loader = Loader.of(file, options);
    this.strategy = new BasicCandidateSelectionStrategy(file.pkg());
    this.parents = new ParentClassFinder(candidates, library, strategy, options);
    // Needed because some other files in the project might extend a class defined in the current
    // file
    library.add(file);
    candidates.add(Candidate.Source.SIBLING, file::findImportables);
  }

  /**
   * Initializes a {@code Fixer} for a {@code file}.
   *
   * @param file the source file to fix
   * @param options the fixer's options
   */
  public static Fixer init(ParsedFile file, Options options) {
    return new Fixer(file, options);
  }

  /**
   * Adds sibling files.
   *
   * <p>This will only add files with the same package as this {@code Fixer}'s file.
   *
   * @param siblings the files to add
   */
  public void addSiblings(Set<ParsedFile> siblings) {
    Set<ParsedFile> siblingsOfSamePackage =
        siblings.stream()
            .filter(s -> s.packageName().equals(file.packageName()))
            .collect(Collectors.toSet());

    loader.addSiblings(siblingsOfSamePackage);
    siblingsOfSamePackage.stream().forEach(f -> candidates.add(Candidate.Source.SIBLING, f));
    siblingsOfSamePackage.stream().forEach(f -> library.add(f));
  }

  public void addStdlibProvider(StdlibProvider provider) {
    loader.addStdlibProvider(provider);
    candidates.add(Candidate.Source.STDLIB, provider);
    library.add(provider);
  }

  public void addEnvironment(Environment environment) {
    loader.addEnvironment(environment);
    candidates.add(Candidate.Source.EXTERNAL, environment);
    library.add(environment);
  }

  private Result loadAndTryToFix(boolean lastTry) {
    var span = Traces.createSpan("Fixer.loadAndTryToFix", new Tag("lastTry", lastTry));
    try (var __ = Traces.activate(span)) {
      return loadAndTryToFixInstrumented(lastTry);
    } finally {
      span.finish();
    }
  }

  private Result loadAndTryToFixInstrumented(boolean lastTry) {
    loader.load();
    if (options.debug()) {
      log.info("load completed");
    }

    if (!file.orphans().needsParents() && file.unresolved().isEmpty()) {
      return Result.complete();
    }

    return fix(lastTry);
  }

  // Given an intermediate load result, use all the candidates gathered so far to find imports to
  // add to the source file.
  //
  // Gives up if all necessary imports cannot be found, except if it is a last try, in which case
  // the best possible incomplete list of fixes will be returned.
  private Result fix(boolean lastTry) {
    var span = Traces.createSpan("Fixer.fix", new Tag("lastTry", lastTry));
    try (var __ = Traces.activate(span)) {
      return fixInstrumented(lastTry);
    } finally {
      span.finish();
    }
  }

  private Result fixInstrumented(boolean lastTry) {
    var allParentsFound = true;
    var fixes = new HashSet<Import>();
    var result = parents.findAllParents(file.orphans());
    if (!result.complete && !lastTry) {
      return Result.incomplete();
    }

    fixes.addAll(
        result.fixes.stream()
            .filter(i -> !i.selector.scope().equals(file.pkg()))
            .map(Import::fromNew)
            .collect(Collectors.toSet()));
    var unresolved = file.unresolved();

    // We had orphan classes, but they did not have any unresolved identifiers
    if (unresolved.isEmpty()) {
      return Result.incomplete(fixes);
    }

    fixes.addAll(findFixes(unresolved, List.of()));
    var allGood = fixes.size() == unresolved.size();

    if (allGood) {
      return Result.complete(fixes);
    }

    return Result.incomplete(fixes);
  }

  private Set<Import> findFixes(Set<Identifier> unresolved, Collection<Import> current) {
    var span = Traces.createSpan("Fixer.findFixes");
    try (var __ = Traces.activate(span)) {
      return findFixesInstrumented(unresolved, current);
    } finally {
      span.finish();
    }
  }

  private Set<Import> findFixesInstrumented(
      Set<Identifier> unresolved, Collection<Import> current) {
    var selectors = unresolved.stream().map(Selector::of).collect(Collectors.toList());
    var candidates = selectors.stream().map(this.candidates::find).reduce(Candidates::merge).get();
    var best = new BasicCandidateSelectionStrategy(file.pkg()).selectBest(candidates);

    return selectors.stream()
        .map(best::forSelector)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(Import::fromNew)
        .collect(Collectors.toSet());
  }

  /**
   * Identifies symbols that really need to be imported, and try to find a fitting import. Either
   * returns a complete result with some fixes, or an incomplete result without any fixes.
   */
  public Result tryToFix() {
    return loadAndTryToFix(false);
  }

  /**
   * Identifies symbols that really need to be imported, and try to find a fitting import. Either
   * returns a complete result with some fixes, or an incomplete result with all the fixes that
   * could be found.
   */
  public Result lastTryToFix() {
    return loadAndTryToFix(true);
  }
}
