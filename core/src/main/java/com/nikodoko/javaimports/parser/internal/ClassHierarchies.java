package com.nikodoko.javaimports.parser.internal;

import com.nikodoko.javaimports.parser.entities.ClassEntity;
import java.util.HashMap;
import java.util.Map;

public class ClassHierarchies {
  public static ClassHierarchy root() {
    return new Root();
  }

  static class Node implements ClassHierarchy {
    private ClassHierarchy parent;
    private ClassEntity entity;
    private Map<String, ClassHierarchy> childs;

    Node(ClassHierarchy parent, ClassEntity entity) {
      this.entity = entity;
      this.parent = parent;
      this.childs = new HashMap<>();
    }

    public ClassHierarchy moveTo(ClassEntity childEntity) {
      Node child = new Node(this, childEntity);
      childs.put(childEntity.name(), child);
      return child;
    }

    public ClassHierarchy moveToLeaf() {
      return new Leaf(this);
    }

    public ClassHierarchy moveUp() {
      return parent;
    }
  }

  static class Leaf implements ClassHierarchy {
    private ClassHierarchy parent;

    Leaf(ClassHierarchy parent) {
      this.parent = parent;
    }

    // We still want to move to a different node, as it is expected that moveUp() will be called as
    // many times as moveToXX will
    public ClassHierarchy moveTo(ClassEntity childEntity) {
      return new Leaf(this);
    }

    public ClassHierarchy moveToLeaf() {
      return new Leaf(this);
    }

    public ClassHierarchy moveUp() {
      return parent;
    }
  }

  static class Root implements ClassHierarchy {
    private Map<String, ClassHierarchy> childs = new HashMap<>();

    public ClassHierarchy moveTo(ClassEntity childEntity) {
      Node child = new Node(this, childEntity);
      childs.put(childEntity.name(), child);
      return child;
    }

    public ClassHierarchy moveToLeaf() {
      return new Leaf(this);
    }

    public ClassHierarchy moveUp() {
      throw new UnsupportedOperationException("cannot move up from root of class hierarchy");
    }
  }
}
