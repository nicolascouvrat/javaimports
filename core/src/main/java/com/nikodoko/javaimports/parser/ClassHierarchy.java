package com.nikodoko.javaimports.parser;

import com.nikodoko.javaimports.parser.internal.ClassEntity;
import com.nikodoko.javaimports.parser.internal.ClassSelector;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A tree view of Java classes.
 *
 * <p>Leaves are non class Java objects that can contain classes (like functions). Leaves have a
 * parent, but are not added to this parent's childs, effectively forming an orphan tree containing
 * classes not reachable from the parent and above.
 */
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

  /**
   * Adds {@code childEntity} to the list of childs of this {@code ClassHierarchy}, and moves to
   * this new child node.
   */
  public ClassHierarchy moveTo(ClassEntity childEntity) {
    ClassHierarchy child = new ClassHierarchy(this, childEntity);
    childs.put(childEntity.name(), child);
    return child;
  }

  /**
   * Creates a leaf, not adding it to the childs of this {@code ClassHierarchy}, and moves to this
   * node.
   */
  public ClassHierarchy moveToLeaf() {
    return notAClass(this);
  }

  /** Moves to the parent of the current {@code ClassHierarchy}. */
  public Optional<ClassHierarchy> moveUp() {
    return Optional.ofNullable(parent);
  }

  /** Tries to find a class in this hierachy using a {@code selector}. */
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
