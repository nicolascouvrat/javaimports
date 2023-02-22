package com.nikodoko.javaimports.common;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** A class still having unresolved identifiers. */
public class OrphanClass {
  public final Set<Identifier> unresolved;
  public final Selector name;
  public final Optional<Superclass> maybeParent;

  public OrphanClass(Selector name, Set<Identifier> unresolved, Superclass parent) {
    this(name, unresolved, Optional.of(parent));
  }

  public OrphanClass(Selector name, Set<Identifier> unresolved, Optional<Superclass> maybeParent) {
    this.maybeParent = maybeParent;
    this.name = name;
    this.unresolved = unresolved;
  }

  public OrphanClass addParent(ClassEntity parent) {
    var stillUnresolved =
        unresolved.stream()
            .filter(identifier -> !parent.declarations.contains(identifier))
            .collect(Collectors.toSet());
    return new OrphanClass(name, stillUnresolved, parent.maybeParent);
  }

  public OrphanClass addDeclarations(Set<Identifier> declarations) {
    var stillUnresolved =
        unresolved.stream().filter(u -> !declarations.contains(u)).collect(Collectors.toSet());
    return new OrphanClass(name, stillUnresolved, maybeParent);
  }

  public boolean hasParent() {
    return maybeParent.isPresent();
  }

  public Superclass parent() {
    if (!hasParent()) {
      throw new IllegalStateException("orphan has no parent: " + this);
    }

    return maybeParent.get();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof OrphanClass)) {
      return false;
    }

    var that = (OrphanClass) o;
    return Objects.equals(this.unresolved, that.unresolved)
        && Objects.equals(this.name, that.name)
        && Objects.equals(this.maybeParent, that.maybeParent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(unresolved, name, maybeParent);
  }

  @Override
  public String toString() {
    return Utils.toStringHelper(this)
        .add("name", name)
        .add("unresolved", unresolved)
        .add("maybeParent", maybeParent)
        .toString();
  }
}
