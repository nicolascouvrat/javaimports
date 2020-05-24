package com.nikodoko.javaimports.parser.entities;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.parser.Scope;
import java.util.List;
import javax.annotation.Nullable;
import org.openjdk.tools.javac.tree.JCTree.JCExpression;

public class EntityImpl implements Entity {
  // The kind of entity
  Kind kind;
  // The entity's visibility
  Visibility visibility;
  // The entity's declared name
  String name;
  // If it is static (default is false)
  boolean isStatic;
  // The scope that goes with this entity, can be null
  Scope scope;
  // The identifiers of the extended class, if any
  // example: for class A extends B.C, this will be [B, C]
  List<String> extendedClassPath;

  EntityImpl(Kind kind, String name, Visibility visibility, boolean isStatic) {
    this.kind = kind;
    this.visibility = visibility;
    this.name = name;
    this.isStatic = isStatic;
  }

  /** Returns the {@code Entity}'s shallow copy. */
  public Entity clone() {
    throw new UnsupportedOperationException();
  }

  /** An {@code Entity}'s declared name */
  @Nullable
  public String name() {
    return name;
  }

  /** The {@link Scope} attached to this {@code Entity} */
  @Nullable
  public Scope scope() {
    return scope;
  }

  /** The kind of this {@code Entity} */
  public Kind kind() {
    return kind;
  }

  /** The path of the extended class of this {@code Entity} */
  @Nullable
  public List<String> extendedClassPath() {
    throw new UnsupportedOperationException();
  }

  /** Set the extended class of this {@code Entity} */
  public void extendedClassPath(List<String> path) {
    throw new UnsupportedOperationException();
  }

  /** Attach a scope to this {@code Entity} */
  public void attachScope(Scope scope) {
    throw new UnsupportedOperationException();
  }

  /** Whether this {@code Entity} is extending anything */
  public boolean isChildClass() {
    throw new UnsupportedOperationException();
  }

  public void registerExtendedClass(JCExpression expr) {
    throw new UnsupportedOperationException();
  }

  /** Debugging support. */
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("kind", kind)
        .add("visibility", visibility)
        .add("isStatic", isStatic)
        .add("scope", scope)
        .add("extendedClassPath", extendedClassPath)
        .toString();
  }
}
