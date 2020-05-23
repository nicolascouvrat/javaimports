package com.nikodoko.javaimports.fixer;

import com.nikodoko.javaimports.parser.Entity;
import com.nikodoko.javaimports.parser.ParsedFile;
import java.util.HashSet;
import java.util.Set;

class Loader {
  private Set<ParsedFile> siblings = new HashSet<>();

  void addSiblings(Set<ParsedFile> siblings) {
    this.siblings = siblings;
  }

  // Try to extend a child class as much as possible (extending its parent if the parent itself is a
  // child, etc).
  boolean tryToExtend(Entity childClass) {
    while (childClass.isChildClass()) {
      if (!tryToExtendOnce(childClass)) {
        // We could not extend it using any of the siblings
        return false;
      }
    }

    // If we reach here, we fully extended this class
    return true;
  }

  // Try to extend a child class using all classes declared in files of the same package
  private boolean tryToExtendOnce(Entity childClass) {
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

      Set<String> unresolved = new HashSet<>();
      for (String s : childClass.scope().notYetResolved()) {
        if (parent.scope().lookup(s) == null) {
          unresolved.add(s);
        }
      }

      childClass.scope().notYetResolved(unresolved);
      childClass.extendedClassPath(parent.extendedClassPath());
      // We managed to extend it once
      return true;
    }

    // Nothing found accross all siblings
    return false;
  }
}
