package com.nikodoko.javaimports.parser;

import static com.google.common.base.Preconditions.checkNotNull;

import com.nikodoko.javaimports.parser.entities.ClassEntity;
import com.nikodoko.javaimports.parser.internal.ClassHierarchy;
import com.nikodoko.javaimports.parser.internal.ClassSelector;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ClassExtender {
  private Set<String> notYetResolved = new HashSet<>();
  private ClassEntity toExtend;
  private Optional<ClassSelector> nextSuperclass;

  private ClassExtender(ClassEntity toExtend, Optional<ClassSelector> nextSuperclass) {
    this.toExtend = toExtend;
    this.nextSuperclass = nextSuperclass;
  }

  public static ClassExtender of(ClassEntity toExtend) {
    return new ClassExtender(toExtend, toExtend.superclass());
  }

  public ClassExtender notYetResolved(Set<String> identifiers) {
    checkNotNull(identifiers, "ClassExtender does not accept null for unresolved identifiers");
    this.notYetResolved = identifiers;
    return this;
  }

  public void resolveUsing(Scope scope) {
    Set<String> unresolved = new HashSet<>();
    for (String s : notYetResolved) {
      if (scope.lookup(s) == null) {
        unresolved.add(s);
      }
    }

    notYetResolved = unresolved;
  }

  public Set<String> notYetResolved() {
    return notYetResolved;
  }

  public void extendAsMuchAsPossibleUsing(ClassHierarchy hierarchy) {
    while (nextSuperclass.isPresent()) {
      Optional<ClassEntity> maybeParent = hierarchy.find(nextSuperclass.get());
      if (!maybeParent.isPresent()) {
        return;
      }

      extendWith(maybeParent.get());
      nextSuperclass = maybeParent.get().superclass();
    }
  }

  public boolean isFullyExtended() {
    return !nextSuperclass.isPresent();
  }

  private void extendWith(ClassEntity parent) {
    Set<String> unresolved = new HashSet<>();
    for (String s : notYetResolved) {
      if (!parent.members().contains(s)) {
        unresolved.add(s);
      }
    }

    notYetResolved = unresolved;
  }
}
