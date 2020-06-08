package com.nikodoko.javaimports.parser;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map;

/** Utility methods for {@link ClassHierarchy}. */
public class ClassHierarchies {
  /** Return an empty {@link ClassHierarchy}. */
  public static ClassHierarchy root() {
    return ClassHierarchy.root();
  }

  /**
   * Combine several {@link ClassHierarchy} into one.
   *
   * <p>Does not accept two hierarchies having childs with the same name.
   */
  public static ClassHierarchy combine(ClassHierarchy... hierarchies) {
    ClassHierarchy root = ClassHierarchy.root();
    for (ClassHierarchy hierarchy : hierarchies) {
      for (Map.Entry<String, ClassHierarchy> child : hierarchy.childs.entrySet()) {
        // If two childs have the same identifier, better crash hard than silently drop one of them
        checkArgument(
            !root.childs.containsKey(child.getKey()),
            "trying to combine two non compatible hierarchies!");

        root.childs.put(child.getKey(), child.getValue());
      }
    }

    return root;
  }
}
