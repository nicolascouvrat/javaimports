package com.nikodoko.javaimports.parser.internal;

import com.nikodoko.javaimports.parser.entities.ClassEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ClassHierarchy {
  ClassHierarchy parent;
  ClassEntity entity;
  Map<String, ClassHierarchy> childs;

  private ClassHierarchy(ClassHierarchy parent, ClassEntity entity) {
    this.entity = entity;
    this.parent = parent;
    this.childs = new HashMap<>();
  }

  static ClassHierarchy root() {
    return new ClassHierarchy(null, null);
  }

  static ClassHierarchy notAClass(ClassHierarchy parent) {
    return new ClassHierarchy(parent, null);
  }

  public ClassHierarchy moveTo(ClassEntity childEntity) {
    ClassHierarchy child = new ClassHierarchy(this, childEntity);
    childs.put(childEntity.name(), child);
    return child;
  }

  public ClassHierarchy moveToLeaf() {
    return notAClass(this);
  }

  public Optional<ClassHierarchy> moveUp() {
    return Optional.ofNullable(parent);
  }

  public Optional<ClassEntity> find(ClassSelector selector) {
    ClassHierarchy candidate = childs.get(selector.selector());
    if (candidate == null) {
      return Optional.empty();
    }

    if (selector.next().isPresent()) {
      return candidate.find(selector.next().get());
    }

    return Optional.of(candidate.entity);
  }
}
