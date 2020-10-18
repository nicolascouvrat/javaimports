package com.nikodoko.javaimports.parser;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import com.nikodoko.javaimports.parser.internal.ClassEntity;
import com.nikodoko.javaimports.parser.internal.ClassSelector;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
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
    checkNotNull(entity);

    this.entity = entity;
    this.parent = parent;
    this.childs = new HashMap<>();
  }

  static ClassHierarchy root() {
    return new ClassHierarchy(null, ClassEntity.NOT_A_CLASS);
  }

  static ClassHierarchy notAClass(ClassHierarchy parent) {
    return new ClassHierarchy(parent, ClassEntity.NOT_A_CLASS);
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

  private void render(Writer w, int indent) throws IOException {
    w.append(Strings.repeat(" ", indent));
    w.append(entity.name() + "\n");

    for (ClassHierarchy c : childs.values()) {
      c.render(w, indent + 2);
    }
  }

  @Override
  public String toString() {
    StringWriter writer = new StringWriter();
    try {
      render(writer, 0);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return writer.toString();
  }
}
