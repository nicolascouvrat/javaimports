package com.nikodoko.javaimports.fixer;

import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.environment.Environment;
import com.nikodoko.javaimports.fixer.internal.LoadResult;
import com.nikodoko.javaimports.fixer.internal.Loader;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.stdlib.StdlibProvider;
import java.util.HashSet;
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
  private static Logger log = Logger.getLogger(Fixer.class.getName());

  private Loader loader;

  private Fixer(ParsedFile file, Options options) {
    this.file = file;
    this.options = options;
    this.loader = Loader.of(file, options);
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
  }

  public void addStdlibProvider(StdlibProvider provider) {
    loader.addStdlibProvider(provider);
  }

  public void addEnvironment(Environment resolver) {
    loader.addEnvironment(resolver);
  }

  private Result loadAndTryToFix(boolean lastTry) {
    loader.load();
    if (options.debug()) {
      log.info("load completed: " + loader.result().toString());
    }

    if (loader.result().isEmpty()) {
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
    var loaded = loader.result();
    if (!loaded.orphans.isEmpty() && !lastTry) {
      return Result.incomplete();
    }

    var unresolved = allUnresolved(loaded);
    var fixes = findFixes(unresolved, loaded);
    var allGood = fixes.size() == unresolved.size();

    if (allGood) {
      return Result.complete(fixes);
    }

    return Result.incomplete(fixes);
  }

  private Set<Import> findFixes(Set<String> unresolved, LoadResult loaded) {
    Set<Import> fixes =
        unresolved.stream()
            .map(loaded.candidates::get)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());

    return fixes;
  }

  private Set<String> allUnresolved(LoadResult loaded) {
    var allUnresolved = new HashSet<String>();
    allUnresolved.addAll(loaded.unresolved);
    loaded.orphans.stream().forEach(o -> allUnresolved.addAll(o.notYetResolved()));
    return allUnresolved;
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
