package com.nikodoko.javaimports.parser.internal;

import com.nikodoko.javaimports.parser.entities.ClassEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ClassHierarchies {
  public static ClassHierarchy root() {
    return new Root();
  }

  static class Node implements ClassHierarchy {
    ClassHierarchy parent;
    ClassEntity entity;
    Map<String, Node> childs;

    Node(ClassHierarchy parent, ClassEntity entity) {
      this.entity = entity;
      this.parent = parent;
      this.childs = new HashMap<>();
    }

    @Override
    public ClassHierarchy moveTo(ClassEntity childEntity) {
      Node child = new Node(this, childEntity);
      childs.put(childEntity.name(), child);
      return child;
    }

    @Override
    public ClassHierarchy moveToLeaf() {
      return new Leaf(this);
    }

    @Override
    public ClassHierarchy moveUp() {
      return parent;
    }

    @Override
    public Optional<ClassEntity> find(ClassSelector selector) {
      Node candidate = childs.get(selector.selector());
      if (candidate == null) {
        return Optional.empty();
      }

      if (selector.next().isPresent()) {
        return candidate.find(selector.next().get());
      }

      return Optional.of(candidate.entity);
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

    public Optional<ClassEntity> find(ClassSelector selector) {
      return Optional.empty();
    }
  }

  static class Root implements ClassHierarchy {
    private Map<String, Node> childs = new HashMap<>();

    @Override
    public ClassHierarchy moveTo(ClassEntity childEntity) {
      Node child = new Node(this, childEntity);
      childs.put(childEntity.name(), child);
      return child;
    }

    @Override
    public ClassHierarchy moveToLeaf() {
      return new Leaf(this);
    }

    @Override
    public ClassHierarchy moveUp() {
      throw new UnsupportedOperationException("cannot move up from root of class hierarchy");
    }

    @Override
    public Optional<ClassEntity> find(ClassSelector selector) {
      Node candidate = childs.get(selector.selector());
      if (candidate == null) {
        return Optional.empty();
      }

      if (selector.next().isPresent()) {
        return candidate.find(selector.next().get());
      }

      return Optional.of(candidate.entity);
    }
  }
}
