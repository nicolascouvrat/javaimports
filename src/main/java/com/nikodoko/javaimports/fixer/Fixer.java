package com.nikodoko.javaimports.fixer;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.parser.Entity;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Fixer {
  ParsedFile file;
  Set<ParsedFile> siblings = new HashSet<>();

  private Fixer(ParsedFile file) {
    this.file = file;
  }

  public static Fixer init(ParsedFile file) {
    return new Fixer(file);
  }

  /**
   * Adds sibling files.
   *
   * <p>This will only add {@code ParsedFile} with the same package as file.
   *
   * @param siblings the files to add
   */
  public void addSiblings(Set<ParsedFile> siblings) {
    this.siblings =
        siblings.stream()
            .filter(s -> s.packageName().equals(file.packageName()))
            .collect(Collectors.toSet());
  }

  private Result loadAndTryToFix(boolean lastTry) {
    LoadResult loaded = load();
    if (loaded.isEmpty()) {
      return Result.complete();
    }

    return fix(loaded, lastTry);
  }

  private Result fix(LoadResult loaded, boolean lastTry) {
    if (!lastTry) {
      return Result.incomplete();
    }

    // TODO: implement
    return null;
  }

  public Result tryToFix() {
    return loadAndTryToFix(false);
  }

  public Result lastTryToFix() {
    return loadAndTryToFix(true);
  }

  private LoadResult load() {
    // Try to resolve with the main file
    Set<String> unresolved = new HashSet<>();
    for (String ident : file.scope().notYetResolved()) {
      if (file.imports().get(ident) == null) {
        unresolved.add(ident);
      }
    }

    if (siblings.isEmpty()) {
      // can't do more
      return new LoadResult(unresolved, file.scope().notYetExtended());
    }

    // TODO: implement
    return null;
  }

  private static class LoadResult {
    public Set<String> unresolved;
    public Set<Entity> orphans;

    public LoadResult(Set<String> unresolved, Set<Entity> orphans) {
      this.unresolved = unresolved;
      this.orphans = orphans;
    }

    public boolean isEmpty() {
      return unresolved.isEmpty() && orphans.isEmpty();
    }

    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("unresolved", unresolved)
          .add("orphans", orphans)
          .toString();
    }
  }

  public static class Result {
    private boolean done;
    private Set<Import> toFix = new HashSet<>();

    Result(boolean done) {
      this.done = done;
    }

    public Set<Import> toFix() {
      return toFix;
    }

    public boolean done() {
      return done;
    }

    static Result complete() {
      return new Result(true);
    }

    static Result incomplete() {
      return new Result(false);
    }

    public String toString() {
      return MoreObjects.toStringHelper(this).add("done", done).add("toFix", toFix).toString();
    }
  }
}
