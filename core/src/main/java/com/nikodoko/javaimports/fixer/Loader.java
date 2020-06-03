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
  private ParsedFile file;

  private Loader(ParsedFile file) {
    this.file = file;
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

  private Set<String> difference(Set<String> original, Set<String> toRemove) {
    Set<String> result = new HashSet<>();
    for (String identifier : original) {
      if (!toRemove.contains(identifier)) {
        result.add(identifier);
      }
    }

    return result;
  }

  // Try to compute what identifiers are still unresolved, and what child classes are still not
  // extended, using whatever information is available to the Fixer at the time.
  LoadResult load() {
    // Try to resolve with the main file imports.
    // FIXME: this does not do anything about the situation where we import a class that we extend
    // in the file. The problem is that we would need informations on the methods provided by said
    // class so that we can decide which identifiers are still unresoled.
    // We should probably have shortcut here that directly goes to find that package? But we most
    // likely need environment information for this...
    Set<String> unresolved = difference(file.scope().notYetResolved(), file.imports().keySet());

    for (ClassExtender e : file.notFullyExtendedClasses()) {
      e.resolveUsing(file.imports().keySet());
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

      unresolved = difference(unresolved, sibling.topLevelDeclarations());

      for (ClassExtender e : file.notFullyExtendedClasses()) {
        e.resolveUsing(sibling.topLevelDeclarations());
      }
    }

    // Finally, try to extend each child class of the original file.
    // If we can finish extending (find all parents), then add whatever is left unresolved to the
    // global set of unresolved identifiers. If it is not totally resolved, add the intermediate
    // result to the list of classes not fully extended
    Set<ClassExtender> notFullyExtendedClasses = new HashSet<>();
    for (ClassExtender e : file.notFullyExtendedClasses()) {
      extendUsingSiblings(e);
      if (e.isFullyExtended()) {
        unresolved.addAll(e.notYetResolved());
        continue;
      }

      notFullyExtendedClasses.add(e);
    }

    return new LoadResult(unresolved, notFullyExtendedClasses);
  }

  private void extendUsingSiblings(ClassExtender toExtend) {
    // XXX: this will stop at the first match in siblings. This should be OK, as siblings are from
    // the same package, so the class selector should not be ambiguous.
    ClassHierarchy[] hierarchies = new ClassHierarchy[siblings.size()];
    int i = 0;
    for (ParsedFile sibling : siblings) {
      hierarchies[i++] = sibling.classHierarchy();
    }

    ClassHierarchy combined = ClassHierarchies.combine(hierarchies);
    toExtend.extendAsMuchAsPossibleUsing(combined);
  }
}
