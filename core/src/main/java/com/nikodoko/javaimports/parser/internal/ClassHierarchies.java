package com.nikodoko.javaimports.parser.internal;

import com.nikodoko.javaimports.parser.entities.ClassEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ClassHierarchies {
  public static ClassHierarchy root() {
    return Node.root();
  }

  static class Node implements ClassHierarchy {
    ClassHierarchy parent;
    ClassEntity entity;
    Map<String, Node> childs;

    private Node(ClassHierarchy parent, ClassEntity entity) {
      this.entity = entity;
      this.parent = parent;
      this.childs = new HashMap<>();
    }

    static Node root() {
      return new Node(null, null);
    }

    static Node notAClass(ClassHierarchy parent) {
      return new Node(parent, null);
    }

    @Override
    public ClassHierarchy moveTo(ClassEntity childEntity) {
      Node child = new Node(this, childEntity);
      childs.put(childEntity.name(), child);
      return child;
    }

    @Override
    public ClassHierarchy moveToLeaf() {
      return notAClass(this);
    }

    @Override
    public Optional<ClassHierarchy> moveUp() {
      return Optional.ofNullable(parent);
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
