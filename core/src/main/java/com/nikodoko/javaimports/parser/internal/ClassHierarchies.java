package com.nikodoko.javaimports.parser.internal;

public class ClassHierarchies {
  public static ClassHierarchy root() {
    return ClassHierarchy.root();
  }

  public static ClassHierarchy combine(ClassHierarchy... hierarchies) {
    ClassHierarchy root = ClassHierarchy.root();
    for (ClassHierarchy hierarchy : hierarchies) {
      root.childs.putAll(hierarchy.childs);
    }

    return root;
  }
}
