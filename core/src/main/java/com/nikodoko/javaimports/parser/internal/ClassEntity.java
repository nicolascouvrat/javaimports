package com.nikodoko.javaimports.parser.internal;

import com.google.common.base.MoreObjects;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

public class ClassEntity {
  private final String name;
  private Set<String> members = new HashSet<>();
  @Nullable private final ClassSelector superclass;

  public static ClassEntity namedAndExtending(String name, ClassSelector superclass) {
    return new ClassEntity(name, superclass);
  }

  public static ClassEntity named(String name) {
    return new ClassEntity(name, null);
  }

  private ClassEntity(String name, ClassSelector superclass) {
    this.name = name;
    this.superclass = superclass;
  }

  public String name() {
    return name;
  }

  public Set<String> members() {
    return members;
  }

  public ClassEntity members(Set<String> members) {
    this.members = members;
    return this;
  }

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
