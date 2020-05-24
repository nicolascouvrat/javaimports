package com.nikodoko.javaimports.parser.entities;

import java.util.Set;
import org.openjdk.javax.lang.model.element.Modifier;
import org.openjdk.source.tree.ModifiersTree;

public class EntityFactory {
  private static class EntityModifiers {
    Visibility visibility = Visibility.NONE;
    boolean isStatic = false;
  }

  public static Entity createClass(String name, ModifiersTree modifiers) {
    return create(Kind.CLASS, name, parseModifiers(modifiers, Kind.METHOD));
  }

  public static Entity createMethod(String name, ModifiersTree modifiers) {
    return create(Kind.METHOD, name, parseModifiers(modifiers, Kind.METHOD));
  }

  public static Entity createTypeParameter(String name) {
    return create(Kind.TYPE_PARAMETER, name, new EntityModifiers());
  }

  public static Entity createVariable(String name, ModifiersTree modifiers) {
    return create(Kind.VARIABLE, name, parseModifiers(modifiers, Kind.VARIABLE));
  }

  public static Entity createBad() {
    return create(Kind.BAD, "", new EntityModifiers());
  }

  private static EntityModifiers parseModifiers(ModifiersTree modifiersTree, Kind kind) {
    EntityModifiers modifiers = new EntityModifiers();
    // XXX: this dictates an arbitrary precedence between public, private and protected in the
    // unlikely case where several of them are present in modifiers at the same time. But this
    // should be forbidden by the java language, so we don't want to bother too much anyway.
    Set<Modifier> flags = modifiersTree.getFlags();

    if (flags.contains(Modifier.PROTECTED)) {
      modifiers.visibility = Visibility.PROTECTED;
    }

    if (flags.contains(Modifier.PRIVATE)) {
      modifiers.visibility = Visibility.PRIVATE;
    }

    if (flags.contains(Modifier.PUBLIC)) {
      modifiers.visibility = Visibility.PUBLIC;
    }

    if (kind == Kind.CLASS) {
      modifiers.visibility = Visibility.PACKAGE_PRIVATE;
    }

    modifiers.isStatic = flags.contains(Modifier.STATIC);
    return modifiers;
  }

  private static Entity create(Kind kind, String name, EntityModifiers modifiers) {
    return new Entity(kind, name, modifiers.visibility, modifiers.isStatic);
  }
}
