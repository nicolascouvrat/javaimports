package com.nikodoko.javaimports.parser.entities;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.parser.internal.ClassSelector;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

public class ClassEntity {
  private final Visibility visibility;
  private final String name;
  private final boolean isStatic;
  @Nullable private final ClassSelector superclass;
  private Set<String> members = new HashSet<>();

  public ClassEntity(Visibility visibility, boolean isStatic, String name) {
    this(visibility, isStatic, name, null);
  }

  public ClassEntity(
      Visibility visibility, boolean isStatic, String name, ClassSelector superclass) {
    this.visibility = visibility;
    this.isStatic = isStatic;
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
        .add("visibility", visibility)
        .add("isStatic", isStatic)
        .add("members", members)
        .add("superclass", superclass)
        .toString();
  }
}
