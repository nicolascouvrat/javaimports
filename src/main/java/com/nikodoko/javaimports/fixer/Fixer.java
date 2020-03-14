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
      for (String ident : unresolved) {
        if (sibling.scope().lookup(ident) != null) {
          unresolved.remove(ident);
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
      if (tryToExtend(childClass)) {
        unresolved.addAll(childClass.scope().notYetResolved());
        continue;
      }

      notYetExtended.add(childClass);
    }

    return new LoadResult(unresolved, notYetExtended);
  }

  private boolean tryToExtend(Entity childClass) {
    while (childClass.isChildClass()) {
      boolean found = false;
      for (ParsedFile sibling : siblings) {
        Entity parent = sibling.scope().findParent(childClass);
        if (parent == null) {
          // Not in this sibling
          continue;
        }

        if (parent.kind() != Entity.Kind.CLASS) {
          // FIXME: what should we do here? it's not really worth continuing, so just return true
          // event though we didnt find it, as a way to say it's no use trying to extend it again.
          return true;
        }

        found = true;
        Set<String> unresolved = new HashSet<>();
        for (String s : childClass.scope().notYetResolved()) {
          if (parent.scope().lookup(s) == null) {
            unresolved.add(s);
          }
        }

        childClass.scope().notYetResolved(unresolved);
        childClass.extendedClassPath(parent.extendedClassPath());
      }

      // If we didn't find anything accross all siblings, break out of the loop
      if (!found) {
        return false;
      }
    }

    return true;
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
