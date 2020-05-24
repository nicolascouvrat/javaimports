package com.nikodoko.javaimports.parser.entities;

import com.google.common.base.MoreObjects;
import javax.annotation.Nullable;

public class EntityImpl implements Entity {
  // The kind of entity
  Kind kind;
  // The entity's visibility
  Visibility visibility;
  // The entity's declared name
  String name;
  // If it is static (default is false)
  boolean isStatic;

  EntityImpl(Kind kind, String name, Visibility visibility, boolean isStatic) {
    this.kind = kind;
    this.visibility = visibility;
    this.name = name;
    this.isStatic = isStatic;
  }

  /** An {@code Entity}'s declared name */
  @Nullable
  public String name() {
    return name;
  }

  /** The kind of this {@code Entity} */
  public Kind kind() {
    return kind;
  }

  /** Debugging support. */
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("kind", kind)
        .add("visibility", visibility)
        .add("isStatic", isStatic)
        .toString();
  }
}
