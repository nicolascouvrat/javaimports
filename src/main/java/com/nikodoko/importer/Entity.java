package com.nikodoko.importer;

import com.google.common.base.MoreObjects;
import com.sun.source.tree.ModifiersTree;
import java.util.Set;
import javax.lang.model.element.Modifier;

public class Entity {
  public static enum Kind {
    FUNCTION,
    VARIABLE,
    CLASS,
    ;
  }

  public static enum Visibility {
    /** The visibility given by {@code public} */
    PUBLIC,
    /** The visibility given by {@code protected} */
    PROTECTED,
    /** The visibility given by {@code private} */
    PRIVATE,
    /** The default visibility for classes (accessible only from the same package) */
    PACKAGE_PRIVATE,
    /** The default visibility for any other scope (accessible only from inside the scope) */
    NONE,
    ;
  }

  // The kind of entity
  Kind kind;
  // The scope that goes with this entity, can be null
  Scope scope;
  // The entity's visibility
  Visibility visibility;
  // The entity's declared name
  String name;
  // If it is static (default is false)
  boolean isStatic;

  /**
   * An {@code Entity} constructor.
   *
   * @param kind its kind
   * @param name its name
   * @param modifiers the different modifiers with which this entity is declared
   */
  public Entity(Kind kind, String name, ModifiersTree modifiers) {
    // XXX: this dictates an arbitrary precedence between public, private and protected in the
    // unlikely case where several of them are present in modifiers at the same time. But this
    // should be forbidden by the java language, so we don't want to bother too much anyway.
    Set<Modifier> flags = modifiers.getFlags();

    if (flags.contains(Modifier.PROTECTED)) {
      this.visibility = Visibility.PROTECTED;
    }

    if (flags.contains(Modifier.PRIVATE)) {
      this.visibility = Visibility.PRIVATE;
    }

    if (flags.contains(Modifier.PUBLIC)) {
      this.visibility = Visibility.PUBLIC;
    }

    // The default visibility depends on the kind
    this.visibility = Visibility.NONE;
    if (kind == Kind.CLASS) {
      this.visibility = Visibility.PACKAGE_PRIVATE;
    }

    this.isStatic = flags.contains(Modifier.STATIC);
    this.kind = kind;
    this.name = name;
  }

  /** An {@code Entity}'s declared name */
  public String name() {
    return name;
  }

  /** Debugging support. */
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("kind", kind)
        .add("visibility", visibility)
        .add("isStatic", isStatic)
        .add("scope", scope)
        .toString();
  }
}
