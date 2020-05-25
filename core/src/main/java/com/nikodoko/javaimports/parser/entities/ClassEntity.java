package com.nikodoko.javaimports.parser.entities;

import com.google.common.base.MoreObjects;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

public class ClassEntity implements Entity {
  private Visibility visibility;
  private String name;
  private boolean isStatic;
  private List<String> parentPath;
  private Set<String> members = new HashSet<>();

  ClassEntity(Visibility visibility, boolean isStatic, String name) {
    this.visibility = visibility;
    this.isStatic = isStatic;
    this.name = name;
  }

  @Override
  public String name() {
    return null;
  }

  @Override
  public Kind kind() {
    return Kind.CLASS;
  }

  public Set<String> members() {
    return members;
  }

  public void members(Set<String> members) {
    this.members = members;
  }

  @Nullable
  public List<String> parentPath() {
    return parentPath;
  }

  /** Set the extended class of this {@code Entity} */
  public void parentPath(List<String> path) {
    parentPath = path;
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
