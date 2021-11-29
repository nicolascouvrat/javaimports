package com.nikodoko.javaimports.parser;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.TreeTraverser;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.parser.internal.ClassEntity;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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

  public static Stream<ClassEntity> flatView(ClassHierarchy root) {
    return TreeTraverser.using(ClassHierarchy::childs).preOrderTraversal(root).stream()
        .map(ClassHierarchy::entity)
        // Skip one because the root is not a proper class
        // TODO: should we change that?
        .skip(1);
  }

  public static Set<com.nikodoko.javaimports.common.ClassEntity> entities(ClassHierarchy root) {
    var r = new HashSet<com.nikodoko.javaimports.common.ClassEntity>();
    for (var child : root.childs()) {
      r.addAll(dig(Optional.empty(), child));
    }

    return r;
  }

  private static Set<com.nikodoko.javaimports.common.ClassEntity> dig(
      Optional<Selector> previous, ClassHierarchy hierarchy) {
    if (hierarchy.entity() == ClassEntity.NOT_A_CLASS) {
      return Set.of();
    }

    var klass = hierarchy.entity().toNew();
    if (!previous.isEmpty()) {
      var scoped = previous.get().combine(klass.name);
      var builder =
          com.nikodoko.javaimports.common.ClassEntity.named(scoped).declaring(klass.declarations);
      if (klass.maybeParent.isPresent()) {
        builder = builder.extending(klass.maybeParent.get());
      }
      klass = builder.build();
    }

    var r = new HashSet<com.nikodoko.javaimports.common.ClassEntity>();
    r.add(klass);
    for (var child : hierarchy.childs()) {
      r.addAll(dig(Optional.of(klass.name), child));
    }

    return r;
  }
}
