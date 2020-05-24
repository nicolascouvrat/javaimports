package com.nikodoko.javaimports.parser.entities;

import java.util.Set;
import org.openjdk.javax.lang.model.element.Modifier;
import org.openjdk.source.tree.ModifiersTree;

public class EntityFactory {
  // EntityModifiers provides sane defaults (except for classes)
  private static class EntityModifiers {
    Visibility visibility = Visibility.NONE;
    boolean isStatic = false;
  }

  public static ScopedClassEntity createClass(String name, ModifiersTree modifiersTree) {
    EntityModifiers modifiers = parseModifiers(modifiersTree);
    return ScopedClassEntity.of(new ClassEntity(modifiers.visibility, modifiers.isStatic, name));
  }

  public static Entity createMethod(String name, ModifiersTree modifiersTree) {
    EntityModifiers modifiers = parseModifiers(modifiersTree);
    return new MethodEntity(modifiers.visibility, modifiers.isStatic, name);
  }

  public static Entity createTypeParameter(String name) {
    return new TypeParameterEntity(name);
  }

  public static Entity createVariable(String name, ModifiersTree modifiersTree) {
    EntityModifiers modifiers = parseModifiers(modifiersTree);
    return new VariableEntity(modifiers.visibility, modifiers.isStatic, name);
  }

  private static EntityModifiers parseClassModifiers(ModifiersTree modifiersTree) {
    EntityModifiers modifiers = new EntityModifiers();
    Set<Modifier> flags = modifiersTree.getFlags();

    modifiers.visibility = getClassVisibility(flags);
    modifiers.isStatic = getStatic(flags);
    return modifiers;
  }

  private static Visibility getClassVisibility(Set<Modifier> flags) {
    Visibility visibility = getVisibility(flags);

    if (visibility == Visibility.NONE) {
      // Default for classes
      return Visibility.PACKAGE_PRIVATE;
    }

    return visibility;
  }

  private static EntityModifiers parseModifiers(ModifiersTree modifiersTree) {
    EntityModifiers modifiers = new EntityModifiers();
    Set<Modifier> flags = modifiersTree.getFlags();

    modifiers.visibility = getVisibility(flags);
    modifiers.isStatic = getStatic(flags);
    return modifiers;
  }

  private static Visibility getVisibility(Set<Modifier> flags) {
    // XXX: this dictates an arbitrary precedence between public, private and protected in the
    // unlikely case where several of them are present in modifiers at the same time. But this
    // should be forbidden by the java language, so we don't want to bother too much anyway.
    if (flags.contains(Modifier.PROTECTED)) {
      return Visibility.PROTECTED;
    }

    if (flags.contains(Modifier.PRIVATE)) {
      return Visibility.PRIVATE;
    }

    if (flags.contains(Modifier.PUBLIC)) {
      return Visibility.PUBLIC;
    }

    // Default for all but class
    return Visibility.NONE;
  }

  private static boolean getStatic(Set<Modifier> flags) {
    return flags.contains(Modifier.STATIC);
  }
}
