package com.nikodoko.javaimports.parser.internal;

import com.google.common.base.MoreObjects;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

/** A representation of Java class, with a name, members, and maybe a pointer to a parent class. */
public class ClassEntity {
  /** A special placeholder for entities that are in fact not a class. */
  public static final ClassEntity NOT_A_CLASS = ClassEntity.named("NOT_A_CLASS");

  private final String name;
  private Set<String> members = new HashSet<>();
  @Nullable private final ClassSelector superclass;

  /** Create a {@code ClassEntity} named {@code name} and extending the given{@code superclass}. */
  public static ClassEntity namedAndExtending(String name, ClassSelector superclass) {
    return new ClassEntity(name, superclass);
  }

  /** Create a {@code ClassEntity} named {@code} name and not extending anything. */
  public static ClassEntity named(String name) {
    return new ClassEntity(name, null);
  }

  private ClassEntity(String name, ClassSelector superclass) {
    this.name = name;
    this.superclass = superclass;
  }

  /** The name of this {@code ClassEntity}. */
  public String name() {
    return name;
  }

  /** The members of this {@code ClassEntity}. */
  public Set<String> members() {
    return members;
  }

  /** Sets the members of this {@code ClassEntity}. */
  public ClassEntity members(Set<String> members) {
    this.members = members;
    return this;
  }

  /** Returns the superclass of this {@code ClassEntity}, if any. */
  public Optional<ClassSelector> superclass() {
    return Optional.ofNullable(superclass);
  }

  /** Debugging support. */
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("members", members)
        .add("superclass", superclass)
        .toString();
  }
}
