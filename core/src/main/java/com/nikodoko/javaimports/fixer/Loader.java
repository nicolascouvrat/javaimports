package com.nikodoko.javaimports.fixer;

import com.nikodoko.javaimports.parser.ClassExtender;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.internal.ClassHierarchies;
import com.nikodoko.javaimports.parser.internal.ClassHierarchy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class Loader {
  private Set<ParsedFile> siblings = new HashSet<>();
  private Map<String, Import> candidates = new HashMap<>();
  private LoadResult result = new LoadResult();
  private ParsedFile file;

  private Loader(ParsedFile file) {
    this.file = file;
    this.result.unresolved = file.notYetResolved();
    this.result.orphans = file.notFullyExtendedClasses();
  }

  static Loader of(ParsedFile file) {
    return new Loader(file);
  }

  void addSiblings(Set<ParsedFile> siblings) {
    this.siblings = siblings;
  }

  Map<String, Import> candidates() {
    return candidates;
  }

  LoadResult result() {
    return result;
  }

  void load() {
    extendAllClasses();
    resolveUsingImports();

    for (ParsedFile sibling : siblings) {
      // Use the imports of sibling files as candidates for the current file
      candidates.putAll(sibling.imports());
      resolveUsingSibling(sibling);
    }
  }

  // FIXME: this does not do anything about the situation where we import a class that we extend
  // in the file. The problem is that we would need informations on the methods provided by said
  // class so that we can decide which identifiers are still unresoled.
  // We should probably have shortcut here that directly goes to find that package? But we most
  // likely need environment information for this...
  private void resolveUsingImports() {
    result.unresolved = difference(result.unresolved, file.imports().keySet());

    for (ClassExtender e : result.orphans) {
      e.resolveUsing(file.imports().keySet());
    }
  }

  private void resolveUsingSibling(ParsedFile sibling) {
    result.unresolved = difference(result.unresolved, sibling.topLevelDeclarations());

    for (ClassExtender e : result.orphans) {
      e.resolveUsing(sibling.topLevelDeclarations());
    }
  }

  private Set<String> difference(Set<String> original, Set<String> toRemove) {
    Set<String> result = new HashSet<>();
    for (String identifier : original) {
      if (!toRemove.contains(identifier)) {
        result.add(identifier);
      }
    }

    return result;
  }

  private void extendAllClasses() {
    Set<ClassExtender> notFullyExtendedClasses = new HashSet<>();
    for (ClassExtender e : result.orphans) {
      extendUsingSiblings(e);
      if (e.isFullyExtended()) {
        result.unresolved.addAll(e.notYetResolved());
        continue;
      }

      notFullyExtendedClasses.add(e);
    }

    result.orphans = notFullyExtendedClasses;
  }

  private void extendUsingSiblings(ClassExtender toExtend) {
    ClassHierarchy[] hierarchies = new ClassHierarchy[siblings.size()];
    int i = 0;
    for (ParsedFile sibling : siblings) {
      hierarchies[i++] = sibling.classHierarchy();
    }

    ClassHierarchy combined = ClassHierarchies.combine(hierarchies);
    toExtend.extendAsMuchAsPossibleUsing(combined);
  }
}
