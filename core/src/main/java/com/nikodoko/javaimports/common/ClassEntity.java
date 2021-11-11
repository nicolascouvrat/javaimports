package com.nikodoko.javaimports.common;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ClassEntity {
  public final Selector name;
  public final Set<Identifier> declarations;
  public final Optional<Superclass> maybeParent;

  private ClassEntity(
      Selector name, Set<Identifier> declarations, Optional<Superclass> maybeParent) {
    this.name = name;
    this.declarations = declarations;
    this.maybeParent = maybeParent;
  }

  public static Builder named(Selector name) {
    return new Builder(name);
  }

  public static class Builder {
    public final Selector name;
    public Set<Identifier> declarations = Set.of();
    public Optional<Superclass> maybeParent = Optional.empty();

    private Builder(Selector name) {
      this.name = name;
    }

    public Builder declaring(Set<Identifier> declarations) {
      this.declarations = declarations;
      return this;
    }

    public Builder extending(Superclass parent) {
      this.maybeParent = Optional.of(parent);
      return this;
    }

    public ClassEntity build() {
      return new ClassEntity(name, declarations, maybeParent);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof ClassEntity)) {
      return false;
    }

    var that = (ClassEntity) o;
    return Objects.equals(this.name, that.name)
        && Objects.equals(this.declarations, that.declarations)
        && Objects.equals(this.maybeParent, that.maybeParent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maybeParent, declarations, name);
  }

  @Override
  public String toString() {
    return Utils.toStringHelper(this)
        .add("name", name)
        .add("declarations", declarations)
        .add("maybeParent", maybeParent)
        .toString();
  }
}
