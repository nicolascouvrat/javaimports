package com.nikodoko.javaimports.fixer;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.parser.Entity;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import java.util.HashSet;
import java.util.Set;

public class Fixer {
  ParsedFile file;
  Set<ParsedFile> siblings;

  private Fixer(ParsedFile file) {
    this.file = file;
  }

  public static Fixer init(ParsedFile file) {
    return new Fixer(file);
  }

  public void addSiblings(Set<ParsedFile> siblings) {
    this.siblings = siblings;
  }

  public Result tryToFix() {
    LoadResult loaded = load();
    if (loaded.isEmpty()) {
      return Result.completed();
    }

    return null;
  }

  private LoadResult load() {
    // Try to resolve with the main file
    Set<String> unresolved = new HashSet<>();
    for (String ident : file.scope().notYetResolved()) {
      if (file.imports().get(ident) == null) {
        unresolved.add(ident);
      }
    }

    if (siblings == null) {
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
  }

  public static class Result {
    private boolean done;
    private Set<Import> toFix;

    Result(boolean done) {
      this.done = done;
      this.toFix = new HashSet<>();
    }

    public Set<Import> toFix() {
      return toFix;
    }

    public boolean done() {
      return done;
    }

    static Result completed() {
      return new Result(true);
    }

    public String toString() {
      return MoreObjects.toStringHelper(this).add("done", done).add("toFix", toFix).toString();
    }
  }
}
