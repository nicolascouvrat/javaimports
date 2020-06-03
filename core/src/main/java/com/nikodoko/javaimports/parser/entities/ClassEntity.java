package com.nikodoko.javaimports.parser.entities;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.parser.internal.ClassSelector;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

public class ClassEntity {
  private final Visibility visibility;
  private final String name;
  private final boolean isStatic;
  @Nullable private final ClassSelector superclass;
  // FIXME: remove me
  private List<String> parentPath;
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

  public Kind kind() {
    return Kind.CLASS;
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

  // FIXME: remove this
  @Nullable
  public List<String> parentPath() {
    return parentPath;
  }

  // FIXME: remove this
  /** Set the extended class of this {@code Entity} */
  public ClassEntity parentPath(List<String> path) {
    this.parentPath = path;
    return this;
  }

  /** Whether this {@code Entity} is extending anything */
  public boolean isChildClass() {
    return parentPath != null;
  }

  public ClassEntity clone() {
    ClassEntity clone = new ClassEntity(visibility, isStatic, name);
    clone.parentPath = parentPath;
    clone.members = members;
    return clone;
  }

  /** Debugging support. */
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("visibility", visibility)
        .add("isStatic", isStatic)
        .add("parentPath", parentPath)
        .add("members", members)
        .toString();
  }
}
