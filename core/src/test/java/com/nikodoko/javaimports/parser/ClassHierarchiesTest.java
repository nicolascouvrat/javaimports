package com.nikodoko.javaimports.parser;

import static com.google.common.truth.Truth8.assertThat;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Superclass;
import com.nikodoko.javaimports.parser.internal.ClassSelectors;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ClassHierarchiesTest {
  static ClassHierarchy root;
  static ClassHierarchy leaf;
  static com.nikodoko.javaimports.parser.internal.ClassEntity A =
      com.nikodoko.javaimports.parser.internal.ClassEntity.named("A");
  static com.nikodoko.javaimports.parser.internal.ClassEntity B =
      com.nikodoko.javaimports.parser.internal.ClassEntity.namedAndExtending(
              "B", ClassSelectors.of("D", "E"))
          .members(Set.of("a", "b"));
  static com.nikodoko.javaimports.parser.internal.ClassEntity C =
      com.nikodoko.javaimports.parser.internal.ClassEntity.named("C");
  static com.nikodoko.javaimports.parser.internal.ClassEntity C1 =
      com.nikodoko.javaimports.parser.internal.ClassEntity.named("C1");
  static com.nikodoko.javaimports.parser.internal.ClassEntity C2 =
      com.nikodoko.javaimports.parser.internal.ClassEntity.named("C2");

  // Create the following:
  // root
  // |
  // - A
  // | |
  // | - B
  // |
  // - C
  //
  // leaf
  // |
  // - C1
  // - C2
  @BeforeAll
  static void setupHierarchy() {
    ClassHierarchy hierarchy = ClassHierarchies.root();
    root = hierarchy;
    hierarchy = hierarchy.moveTo(A);
    hierarchy = hierarchy.moveTo(B);
    hierarchy = hierarchy.moveUp().get();
    hierarchy = hierarchy.moveUp().get();
    hierarchy = hierarchy.moveTo(C);
    hierarchy = hierarchy.moveToLeaf();
    leaf = hierarchy;
    hierarchy = hierarchy.moveTo(C1);
    hierarchy = hierarchy.moveUp().get();
    hierarchy = hierarchy.moveTo(C2);
  }

  @Test
  void testEntitiesConversion() {
    var expected =
        List.of(
            ClassEntity.named(Selector.of("A")).build(),
            ClassEntity.named(Selector.of("A", "B"))
                .extending(Superclass.unresolved(Selector.of("D", "E")))
                .declaring(Set.of(new Identifier("a"), new Identifier("b")))
                .build(),
            ClassEntity.named(Selector.of("C")).build());
    com.google.common.truth.Truth.assertThat(ClassHierarchies.entities(root))
        .containsExactlyElementsIn(expected);
  }

  @Test
  void testFound() {
    assertThat(root.find(ClassSelectors.of("A"))).hasValue(A);
    assertThat(root.find(ClassSelectors.of("A", "B"))).hasValue(B);
    assertThat(root.find(ClassSelectors.of("C"))).hasValue(C);
  }

  @Test
  void testNotFound() {
    assertThat(root.find(ClassSelectors.of("Z"))).isEmpty();
  }

  @Test
  void testLeafNotReachableFromParent() {
    assertThat(root.find(ClassSelectors.of("C", "C1"))).isEmpty();
    assertThat(root.find(ClassSelectors.of("C", "C2"))).isEmpty();
  }

  @Test
  void testLeafChildsCanReachEachother() {
    assertThat(leaf.find(ClassSelectors.of("C1"))).hasValue(C1);
    assertThat(leaf.find(ClassSelectors.of("C2"))).hasValue(C2);
  }

  @Test
  void testCombineLeafAndRoot() {
    ClassHierarchy combined = ClassHierarchies.combine(root, leaf);

    assertThat(leaf.find(ClassSelectors.of("C1"))).hasValue(C1);
    assertThat(leaf.find(ClassSelectors.of("C2"))).hasValue(C2);
    assertThat(root.find(ClassSelectors.of("A"))).hasValue(A);
    assertThat(root.find(ClassSelectors.of("A", "B"))).hasValue(B);
  }

  @Test
  void testFlatView() {
    assertThat(ClassHierarchies.flatView(root)).containsExactly(A, B, C).inOrder();
  }
}
