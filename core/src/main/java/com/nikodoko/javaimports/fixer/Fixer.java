package com.nikodoko.javaimports.fixer;

import com.nikodoko.javaimports.parser.Entity;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Given a {@link ParsedFile} with unresolved identifiers and orphan child classes, use a variety of
 * information to determine which identifiers indeed need to be imported, and how to import them.
 */
public class Fixer {
  private ParsedFile file;
  private FixerOptions options;
  private Set<ParsedFile> siblings = new HashSet<>();
  private Map<String, Import> candidates = new HashMap<>();
  private static Logger log = Logger.getLogger(Fixer.class.getName());

  private Loader loader;

  private Fixer(ParsedFile file, FixerOptions options) {
    this.file = file;
    this.options = options;
    this.loader = new Loader();
  }

  /**
   * Initializes a {@code Fixer} for a {@code file}.
   *
   * @param file the source file to fix
   * @param options the fixer's options
   */
  public static Fixer init(ParsedFile file, FixerOptions options) {
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

    this.siblings = siblingsOfSamePackage;
    loader.addSiblings(siblingsOfSamePackage);
  }

  private Result loadAndTryToFix(boolean lastTry) {
    LoadResult loaded = load();
    if (options.debug()) {
      log.info("load completed: " + loaded.toString());
    }

    if (loaded.isEmpty()) {
      return Result.complete();
    }

    return fix(loaded, lastTry);
  }

  // Given an intermediate load result, use all the candidates gathered so far to find imports to
  // add to the source file.
  //
  // Gives up if all necessary imports cannot be found, except if it is a last try, in which case
  // the best possible incomplete list of fixes will be returned.
  private Result fix(LoadResult loaded, boolean lastTry) {
    boolean allGood = true;
    Set<Import> fixes = new HashSet<>();
    for (String ident : loaded.unresolved) {
      Import fix = candidates.get(ident);
      if (fix != null) {
        fixes.add(fix);
        continue;
      }

      allGood = false;
    }

    // We have found all necessary fixes
    if (allGood && loaded.orphans.isEmpty()) {
      return Result.complete(fixes);
    }

    // This is not the last try, we can probably find more fixes next try
    if (!lastTry) {
      return Result.incomplete();
    }

    // We did not find everything, do our best effort by trying to resolve anything we can in non
    // resolved orphan classes
    for (Entity orphan : loaded.orphans) {
      for (String ident : orphan.scope().notYetResolved()) {
        if (candidates.get(ident) != null) {
          fixes.add(candidates.get(ident));
        }
      }
    }

    return Result.incomplete(fixes);
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

  // Try to compute what identifiers are still unresolved, and what child classes are still not
  // extended, using whatever information is available to the Fixer at the time.
  private LoadResult load() {
    // Try to resolve with the main file imports.
    // FIXME: this does not do anything about the situation where we import a class that we extend
    // in the file. The problem is that we would need informations on the methods provided by said
    // class so that we can decide which identifiers are still unresoled.
    // We should probably have shortcut here that directly goes to find that package? But we most
    // likely need environment information for this...
    Set<String> unresolved = new HashSet<>();
    for (String ident : file.scope().notYetResolved()) {
      if (file.imports().get(ident) == null) {
        unresolved.add(ident);
      }
    }

    for (Entity childClass : file.scope().notYetExtended()) {
      Set<String> notYetResolved = new HashSet<>();
      for (String ident : childClass.scope().notYetResolved()) {
        if (file.imports().get(ident) == null) {
          notYetResolved.add(ident);
        }
      }

      childClass.scope().notYetResolved(notYetResolved);
    }

    // Go over all identifiers in siblings and resolve what we can.
    // XXX: We also try to resolve not yet extended classes, as there are two possibilities:
    //  - that identifier is accessible in the package, but not defined in the parent class, so we
    //  need to resolve it there
    //  - that identifier is accessible in the package and shadowed by the parent class, but it does
    //  not change the fact that it can be resolved anyway.
    for (ParsedFile sibling : siblings) {
      // Add all the imports in that file to the list of potential candidates
      candidates.putAll(sibling.imports());

      for (Iterator<String> i = unresolved.iterator(); i.hasNext(); ) {
        if (sibling.scope().lookup(i.next()) != null) {
          i.remove();
        }
      }

      for (Entity childClass : file.scope().notYetExtended()) {
        Set<String> notYetResolved = new HashSet<>();
        for (String ident : childClass.scope().notYetResolved()) {
          if (sibling.scope().lookup(ident) == null) {
            notYetResolved.add(ident);
          }
        }

        childClass.scope().notYetResolved(notYetResolved);
      }
    }

    // Finally, try to extend each child class of the original file.
    // If we can finish extending (find all parents), then add whatever is left unresolved to the
    // global set of unresolved identifiers. If it is not totally resolved, add the intermediate
    // result to the list of classes not fully extended
    Set<Entity> notYetExtended = new HashSet<>();
    for (Entity childClass : file.scope().notYetExtended()) {
      if (loader.tryToExtend(childClass)) {
        unresolved.addAll(childClass.scope().notYetResolved());
        continue;
      }

      notYetExtended.add(childClass);
    }

    return new LoadResult(unresolved, notYetExtended);
  }
}
