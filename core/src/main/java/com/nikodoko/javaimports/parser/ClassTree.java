package com.nikodoko.javaimports.parser;

import static com.nikodoko.javaimports.common.Utils.checkNotNull;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Selector;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A tree view of Java classes.
 *
 * <p>Leaves are non class Java objects that can contain classes (like functions). Leaves have a
 * parent, but are not added to this parent's childs, effectively forming an orphan tree containing
 * classes not reachable from the parent and above.
 */
public class ClassTree {
  // Used to represent anything that can have nested classes, but is not a class (and therefore
  // invisible from the outside)
  private static final ClassEntity NOT_A_CLASS =
      ClassEntity.named(Selector.of("NOT_A_CLASS")).build();
  // Used to represent the java file itself, as it is possible to declare multiple classes in the
  // same file
  private static final ClassEntity ROOT = ClassEntity.named(Selector.of("ROOT")).build();

  private ClassEntity entity;
  private final ClassTree parent;
  // We want values to be presented in order of insertion
  private final LinkedList<ClassTree> childs;

  private ClassTree(ClassTree parent, ClassEntity entity) {
    checkNotNull(entity);
    this.entity = entity;
    this.parent = parent;
    this.childs = new LinkedList<>();
  }

  public static ClassTree root() {
    return new ClassTree(null, ROOT);
  }

  static ClassTree notAClass(ClassTree parent) {
    return new ClassTree(parent, NOT_A_CLASS);
  }

  public Optional<ClassEntity> find(Selector selector) {
    var maybeCandidate =
        childs.stream().filter(n -> selector.startsWith(n.entity.name)).findFirst();

    if (maybeCandidate.isEmpty()) {
      if (entity.name.equals(selector)) {
        return Optional.of(entity);
      }

      return Optional.empty();
    }

    var candidate = maybeCandidate.get();
    if (candidate.entity.name.equals(selector)) {
      return Optional.of(candidate.entity);
    }

    return candidate.find(selector.rebase(candidate.entity.name));
  }

  public ClassTree pushAndMoveDown(ClassEntity child) {
    var next = new ClassTree(this, child);
    childs.add(next);
    return next;
  }

  public ClassTree moveDown() {
    return notAClass(this);
  }

  public ClassTree moveUp() {
    if (parent == null) {
      throw new IllegalStateException("Cannot move up, currently at root");
    }

    return parent;
  }

  // Workaround the fact that ClassEntity is immutable
  public void addDeclarations(Set<Identifier> declarations) {
    var allDeclarations =
        Stream.concat(declarations.stream(), entity.declarations.stream())
            .collect(Collectors.toSet());
    var updated = ClassEntity.named(entity.name).declaring(allDeclarations);
    if (entity.maybeParent.isPresent()) {
      updated.extending(entity.maybeParent.get());
    }

    entity = updated.build();
  }

  /**
   * Returns a map of {@code selector:entity} such that if the entity of this node is in a package
   * {@code pkg}, {@code pkg.combine(selector)} gives the import path to {@code entity}.
   */
  public Map<Selector, ClassEntity> flatView() {
    if (entity != ROOT) {
      return flatten(Optional.empty());
    }

    // If we're at the root, only look at children
    var flattenedView = new HashMap<Selector, ClassEntity>();
    for (var child : childs) {
      flattenedView.putAll(child.flatten(Optional.empty()));
    }

    return flattenedView;
  }

  private Map<Selector, ClassEntity> flatten(Optional<Selector> maybeScope) {
    if (entity == ROOT) {
      throw new IllegalStateException("Found ROOT not at the top of the tree");
    }

    if (entity == NOT_A_CLASS) {
      return Map.of();
    }

    var flattenedView = new HashMap<Selector, ClassEntity>();
    var scope = maybeScope.map(s -> s.combine(entity.name)).orElse(entity.name);
    flattenedView.put(scope, entity);
    for (var child : childs) {
      flattenedView.putAll(child.flatten(Optional.of(scope)));
    }

    return flattenedView;
  }
}
