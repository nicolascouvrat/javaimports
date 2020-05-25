package com.nikodoko.javaimports.fixer;

import static com.google.common.base.Preconditions.checkArgument;

import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.entities.ScopedClassEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

  // Try to compute what identifiers are still unresolved, and what child classes are still not
  // extended, using whatever information is available to the Fixer at the time.
  LoadResult load() {
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

    for (ScopedClassEntity childClass : file.scope().notYetExtended()) {
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

      for (ScopedClassEntity childClass : file.scope().notYetExtended()) {
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
    Set<ScopedClassEntity> notYetExtended = new HashSet<>();
    for (ScopedClassEntity childClass : file.scope().notYetExtended()) {
      extend(childClass);
      if (!isExtendable(childClass)) {
        unresolved.addAll(childClass.scope().notYetResolved());
        continue;
      }

      notYetExtended.add(childClass);
    }

    return new LoadResult(unresolved, notYetExtended);
  }

  // Try to extend a child class as much as possible (extending its parent if the parent itself is a
  // child, etc).
  private void extend(ScopedClassEntity childClass) {
    while (isExtendable(childClass)) {
      List<ScopedClassEntity> possibleParents = findPossibleParents(childClass);
      if (possibleParents.isEmpty()) {
        return;
      }

      extendWith(childClass, bestParent(possibleParents, childClass));
    }
  }

  private boolean isExtendable(ScopedClassEntity childClass) {
    return childClass.isChildClass();
  }

  private List<ScopedClassEntity> findPossibleParents(ScopedClassEntity childClass) {
    List<ScopedClassEntity> possibleParents = new ArrayList<>();
    for (ParsedFile sibling : siblings) {
      ScopedClassEntity parent = sibling.scope().findParent(childClass);
      if (parent == null) {
        continue;
      }

      possibleParents.add(parent);
    }

    return possibleParents;
  }

  private ScopedClassEntity bestParent(
      List<ScopedClassEntity> possibleParents, ScopedClassEntity child) {
    // FIXME: this assumes we have only one parent
    checkArgument(!possibleParents.isEmpty(), "cannot find best parent in empty list");
    return possibleParents.get(0);
  }

  private void extendWith(ScopedClassEntity child, ScopedClassEntity parent) {
    Set<String> unresolved = new HashSet<>();
    for (String s : child.scope().notYetResolved()) {
      if (parent.scope().lookup(s) == null) {
        unresolved.add(s);
      }
    }

    child.scope().notYetResolved(unresolved);
    child.parentPath(parent.parentPath());
  }
}
