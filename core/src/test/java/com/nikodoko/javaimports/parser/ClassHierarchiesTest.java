package com.nikodoko.javaimports.parser;

import static com.google.common.truth.Truth8.assertThat;

import com.nikodoko.javaimports.parser.internal.ClassEntity;
import com.nikodoko.javaimports.parser.internal.ClassSelectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ClassHierarchiesTest {
  static ClassHierarchy root;
  static ClassHierarchy leaf;
  static ClassEntity A = ClassEntity.named("A");
  static ClassEntity B = ClassEntity.named("B");
  static ClassEntity C = ClassEntity.named("C");
  static ClassEntity C1 = ClassEntity.named("C1");
  static ClassEntity C2 = ClassEntity.named("C2");

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
