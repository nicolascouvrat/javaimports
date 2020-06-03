package com.nikodoko.javaimports.parser.entities;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.parser.internal.ClassSelector;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

public class ClassEntity {
  private final String name;
  private Set<String> members = new HashSet<>();
  @Nullable private final ClassSelector superclass;

  public ClassEntity(String name) {
    this(name, null);
  }

  public ClassEntity(String name, ClassSelector superclass) {
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
