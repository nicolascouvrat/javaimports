package com.nikodoko.javaimports.parser;

import static com.google.common.base.Preconditions.checkNotNull;

import com.nikodoko.javaimports.parser.internal.ClassEntity;
import com.nikodoko.javaimports.parser.internal.ClassSelector;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Wrapper around a {@link ClassEntity} to handle progressive extension, as well as identifier
 * resolution.
 */
public class ClassExtender {
  private Set<String> notYetResolved = new HashSet<>();
  private final ClassEntity toExtend;
  private Optional<ClassSelector> nextSuperclass;

  private ClassExtender(ClassEntity toExtend, Optional<ClassSelector> nextSuperclass) {
    this.toExtend = toExtend;
    this.nextSuperclass = nextSuperclass;
  }

  /** Wrap a given {@link ClassEntity} into a {@code ClassExtender}. */
  public static ClassExtender of(ClassEntity toExtend) {
    return new ClassExtender(toExtend, toExtend.superclass());
  }

  /** Sets unresolved identifiers associated with this {@code ClassExtender}. */
  public ClassExtender notYetResolved(Set<String> identifiers) {
    checkNotNull(identifiers, "ClassExtender does not accept null for unresolved identifiers");
    this.notYetResolved = identifiers;
    return this;
  }

  /** Resolve all identifiers that appear in {@code identifiers}. */
  public void resolveUsing(Set<String> identifiers) {
    Set<String> unresolved = new HashSet<>();
    for (String s : notYetResolved) {
      if (!identifiers.contains(s)) {
        unresolved.add(s);
      }
    }

    notYetResolved = unresolved;
  }

  /** Returns the unresolved identifiers associated with this {@code ClassExtender}. */
  public Set<String> notYetResolved() {
    return notYetResolved;
  }

  /**
   * Use the classes contained in a {@code hierarchy} to extend this {@code ClassExtender} as much
   * as possible, resolving identifiers along the way.
   */
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

  /** Returns true if this {@code ClassExtender} does not need further extending. */
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
